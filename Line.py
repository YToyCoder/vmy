# print file row number and line number
import sys
def readDo(filename : str):
	with open(filename) as file:
		line = file.readline()
		rowCount = 1
		while line:
			format_print(line, rowCount)
			rowCount += 1
			line = file.readline()
	# pass
def print_row_number(num : int):
	print(f"{num:04}",end="")
def format_print(line : str, rowNumber : int):
	# print(f"{rowNumber:04} {line}")
	print_row_number(rowNumber)
	for i,c in enumerate(line):
		print(f"{c:>3}",end="")
	print()
	print_row_number(rowNumber)
	for i in range(len(line)):
		print(f"{i:>3}",end="")
	print()
	pass

if __name__ == '__main__':
	argv = sys.argv
	assert len(argv) > 1
	readDo(argv[1])