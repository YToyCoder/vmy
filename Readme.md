### vmy language

*当前: rebuilt with scala*

#### 1. 目的

了解计算机编译原理

#### 2. 该语言主要特点

##### 2.1 目前支持的特点

1. 内建类型 : `Int`, `Double`, `Bool`, `Char`, `String`, `Function`.
2. 数字计算 : "1 + 2 * (3 - 4)"
3. 变量声明 : let,val. let 是可变变量, val 是不可变变量
4. 字面量 ： "12", "1.01", ""string literal"", "true" 
5. 内建函数 : "print" . "print("hello, world")"
6. bool类型比较 : "<", ">", ">=", "=="
7. array(数组): 数组字面量、数组迭代、元素跟新和添加
8. 运行脚本
9. 注释: #
10. if-else : if, elif, else
11. 函数声明 : function
12. 局部变量 : local variable
13. VmyObject : Vmy结构对象支持， 字面量初始化，成员获取和更新
14. closure(闭包)

##### 2.2 计划支持的特点

#### 2.3 example (示例)

##### 2.3.1 function declaration (函数声明)
函数声明开始于`fn`或者`fun`

```
fn fnDecl() 
{
    let a = 1 + 2
    return a ++ ":fnDecl"
}

fun funDecl()
{
    println("funDecl")
}

```

##### 2.3.2 array

*数组字面量*
```
let a = [1,2,3]
```

*跟新数组元素*
```
let a = [1, 2, 3]
a(0) = 100 # [100, 2, 3]
```

*添加数组元素*
```
let a = [1,2]
a += 3 # [1,2,3]
```

##### 2.3.3 obj

"VmyObj"
```
let a = {name: "Tom", }
fn GetName() 
{
    return a("name")
}
GetName()

a("GetName") = GetName

```

##### 2.3.4 closure support (闭包)

```
fn outfn()
{
    let variable = 1.0
    fn infn() {
        variable += 4
        println("variable is => " ++ variable)
    }
    return infn
}
val a = outfn()
a()
a()

```

##### 2.3.5 for

*use for with array or range-function*
```

for i in range(10){
  println("range " ++ i)
}

for value, index in range(10){
    println("range " ++ value ++ " index " ++ index)
}

let arr = [1,2,3,4]

for i in arr {
    println(i)
}

let k = []

for i in range(10){
    k + i
}

println(k)

```

##### 2.3.6 全局对象

*__G*
```

println(__G)

```

##### 2.3.6 export

```
let a = 1
export a

let ex = {a:"string-literal"}

export ex as EX

fn k() {
    println("k-fn")
}
export {
    a as A,
    k
}
```

##### 2.3.6 import

file => a.vmy
```

let a = 0

export a

let obj = {name: "name", age: "18"}

export obj as VObj
```

fille => main.vmy
```

import {a as A, VObj } from "a.vmy"

println(A)
println(VObj)

```