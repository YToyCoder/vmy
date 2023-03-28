package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.Identifiers;
import com.silence.vmy.compiler.LexicalException;
import com.silence.vmy.compiler.TokenHistoryRecorder;
import com.silence.vmy.tools.Utils;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * a new version handler to handle the file by NIO
 */
public class FileInputScanner implements Scanner, AutoCloseable {

    public FileInputScanner(
            String file_path
    ) throws FileNotFoundException {

        this(file_path, true);

    }

    public FileInputScanner(
            String file_path_or_pure_string,
            boolean is_file_path
    ) throws FileNotFoundException {

        this.file_path = file_path_or_pure_string;
        init(file_path_or_pure_string, is_file_path);

    }

    /**
     * init resource
     *
     * @param filename_or_string_expression
     * @throws FileNotFoundException
     */
    private void init(
            String filename_or_string_expression,
            boolean is_file_name
    ) throws FileNotFoundException {

        channel = getChannel(filename_or_string_expression, is_file_name);
        buffer = ByteBuffer.wrap(new byte[1028]);
        buffer.flip();
        pos = 0;
        cs = new LinkedList<>();
        tokens = new LinkedList<>();
        lineN = 0;

    }

    /**
     * get {@link ReadableByteChannel} from file or pure string
     *
     * @param filename_or_string_expression
     * @param is_name
     * @return {@link ReadableByteChannel }
     * @throws FileNotFoundException
     */
    private ReadableByteChannel getChannel(
            String filename_or_string_expression,
            boolean is_name
    ) throws FileNotFoundException {

        if (is_name) {
            origin = new RandomAccessFile(filename_or_string_expression, "rw");
            return origin.getChannel();
        } else {
            arr_origin = new ByteArrayInputStream(filename_or_string_expression.getBytes());
            return Channels.newChannel(arr_origin);
        }

    }

    private ReadableByteChannel channel;
    private RandomAccessFile origin;
    private ByteArrayInputStream arr_origin;
    private List<Token> tokens;
    private ByteBuffer buffer;
    private final String file_path;
    private int pos;
    private int record;
    private int lineN; // line number
    private LinkedList<Character> cs;
    private boolean end_of_file;
    private TokenHistoryRecorder token_history_recorder;

    @Override
    public List<Token> scan(String source) {
        List<Token> tos = new LinkedList<>();
        while (hasNext())
            tos.add(next());
        return tos;
    }

    @Override
    public Token peek() {
        checkNotEmpty();
        return tokens.get(0);
    }

    @Override
    public Token next() {
        checkNotEmpty();
        return Objects.isNull(token_history_recorder) ?
                tokens.remove(0) :
                token_history_recorder.record_to_history(tokens.remove(0)).last();
    }

    @Override
    public boolean hasNext() {
        checkNotEmpty();
        return !tokens.isEmpty();
    }


    /**
     * check if the token list is empty, if empty and has char , then add new token to token list
     */
    private void checkNotEmpty() {
        while (tokens.isEmpty() && has_char())
            do_fill_tokens();
    }

    /**
     * fill the tokens
     */
    private void do_fill_tokens() {
        if (has_char()) {
            switch (peek_char()) {
                case '"': // string literal
                    handle_string_literal();
                    break;
                case '#':
                    handle_annotation();
                    break;
                case Identifiers.SingleQuote:
                    break;
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '0':
                    handle_digit_literal();
                    break;
                case ' ':
                    handle_black();
                    break;
                case Identifiers.OpenBraceChar:
                case Identifiers.ClosingBraceChar:
                case ',': // Comma
                case '(':
                    handle_single_char_identifier();
                    break;
                default:
                    // identifier :
                    if (Identifiers.identifiers.contains(peek_char()))
                        handle_identifier_kind();
                    else if (is_end_of_line())
                        handle_end_of_line();
                    else if (
                            Identifiers.operatorCharacters.contains(peek_char()) ||
                                    Identifiers.commonIdentifiers.contains(peek_char())
                    ) handle_operator();
                    else
                        throw new LexicalException(
                                /* position */pos(),
                                /* file */file_path,
                                "can't handle char : " + peek_char() + " at position " + pos
                        );
            }
        }
    }

    /**
     * current offset from first word
     *
     * @return
     */
    private int pos() {
        return pos;
    }

    /**
     * handle annotation which start with "#“
     */
    private void handle_annotation() {
        while (has_char() && !is_end_of_line())
            next_char();
    }

    /**
     * handle the string literal , like : "string literal"
     */
    private void handle_string_literal() {

        record_position();
        next_char(); // remove "

        StringBuilder builder = new StringBuilder().append('"');
        while (
                has_char() &&
                        // not quote
                        !Utils.equal(peek_char(), Identifiers.Quote) &&
                        // not end of line
                        !is_end_of_line()
        ) builder.append(next_char());

        if (
                !has_char() ||
                        !Utils.equal(next_char(), Identifiers.Quote)
        ) /* try to remove " */
            throw new LexicalException(
                    get_record(),
                    file_path,
                    "string literal has no closing quote"
            );

        tokens.add(
                new Token(
                        Token.Literal,
                        builder.append('"').toString(),
                        get_record()
                )
        );

    }

    /**
     * handle digit literal , like :
     * 1 , 2.0, 100
     */
    private void handle_digit_literal() {
        record_position();

        final StringBuilder builder = new StringBuilder();
        while (has_char() && Character.isDigit(peek_char()))
            builder.append(next_char());

        if (has_char() && Utils.equal(peek_char(), Identifiers.Dot)) {
            // double
            builder.append(next_char());
            while (has_char() && Character.isDigit(peek_char()))
                builder.append(next_char());
        }

        tokens.add(
                new Token(
                        Token.Literal,
                        builder.toString(),
                        get_record()
                )
        );

    }

    private boolean is_qid_char(char c){
        return Character.isDigit(c) || Character.isAlphabetic(c) || c == '_';
    }

    /**
     * identifier things, variable name , function name or declaration
     */
    private void handle_identifier_kind() {
        record_position();

        final StringBuilder builder = new StringBuilder();
        while (
                has_char() && is_qid_char(peek_char())
        ) builder.append(next_char());

        final String identifier = builder.toString();
        tokens.add(
                new Token(
                        get_identifier_kind(identifier),
                        identifier,
                        get_record()
                )
        );

    }

    private void handle_operator() {
        record_position();

        final StringBuilder builder = new StringBuilder();
        while (
            has_char() &&
            (
                Identifiers.operatorCharacters.contains(peek_char()) ||
                Identifiers.commonIdentifiers.contains(peek_char())
            )
        ) builder.append(next_char());

        final String operator = builder.toString();
        tokens.add(
                new Token(
                        get_identifier_kind(operator),
                        operator,
                        get_record()
                )
        );

    }

    private int get_identifier_kind(String identifier) {

        return switch (identifier) {
            case /* let , val */
                Identifiers.ConstDeclaration,
                Identifiers.VarDeclaration -> Token.Declaration;

            case /* = */ Identifiers.Assignment -> Token.Assignment;

            case /* while, if, elif, else */
                Identifiers.While,
                Identifiers.If,
                Identifiers.Elif,
                Identifiers.Else -> Token.Builtin;

            case /* true false */
                Identifiers.True, Identifiers.False -> Token.Literal;

            default -> {
                if (Identifiers.builtinCall.contains(identifier)) yield Token.BuiltinCall;
                yield Token.Identifier;
            }
        };

    }

    /**
     * handle builtin character which is single character
     */
    private void handle_single_char_identifier() {

        tokens.add(
                new Token(
                        Token.Identifier,
                        Character.toString(next_char()),
                        pos()
                )
        );

    }

    /**
     * record current position
     */
    private void record_position() {
        record = pos();
    }

    /**
     * get the recorded position
     *
     * @return recorded position
     * @see FileInputScanner#get_record()
     */
    private int get_record() {
        return lineN;
    }

    /**
     * remove the black between two token
     */
    private void handle_black() {

        while (
                has_char() &&
                        Utils.equal(peek_char(), ' ')
        ) next_char();

    }

    private boolean is_end_of_line() {

        if (
                has_char() &&
                        Utils.equal(cs.peek(), '\n')
        ) return true;

        // check if next two char is "\r\n"
        char next_char = next_char(); // move out
        boolean end_of_line = (Utils.equal(next_char, '\r') && Utils.equal(peek_char(), '\n'));
        cs.addFirst(next_char); // set back
        return end_of_line;

    }

    private void handle_end_of_line() {
        record_position();

        StringBuilder builder = new StringBuilder();
        if (!Utils.equal(peek_char() /* may be '\n' or '\r\n'*/, '\n')) {
            builder.append(next_char()).append(next_char());
        } else builder.append(next_char());

        tokens.add(
                new Token(
                        Token.NewLine,
                        builder.toString(),
                        pos()
                )
        );
        // increase line number
        lineN++;
    }

    private boolean has_char() {

        if (!cs.isEmpty() || buffer.hasRemaining())
            return true;
        if (end_of_file) /* already at end_of_file */
            return false;

        if(!channel.isOpen())
            return false;

        buffer.clear();
        try {
            end_of_file = channel.read(buffer) == 0;
        } catch (IOException e) {
            e.printStackTrace();
            throw new LexicalException(
                    pos(),
                    file_path,
                    e.getMessage()
            );
        }
        buffer.flip();
        return buffer.hasRemaining();

    }

    private char peek_char() {
        check_and_set_char();
        return cs.peek();
    }

    private char next_char() {
        check_and_set_char();
        pos++;
        return cs.pop();
    }

    /**
     * call this after @see hash_char
     */
    private void check_and_set_char() {
        while (cs.isEmpty())
            cs.add((char) buffer.get());
    }

    @Override
    public boolean register(
            TokenHistoryRecorder recorder,
            boolean force
    ) {

        if (force) {
            this.token_history_recorder = recorder;
            return true;
        }

        if (Objects.nonNull(this.token_history_recorder))
            return false;

        token_history_recorder = recorder;
        return true;

    }

    @Override
    public void close() throws Exception {

        if (Objects.nonNull(origin))
            origin.close();
        if (Objects.nonNull(arr_origin))
            arr_origin.close();

    }
}
