// LiveTemplates params use : groovyScript("/.../MethodName.groovy", filePath(), lineNumber())

def goFilePath = _1
def tagLineNum = Integer.parseInt(_2)

// println  'goFilePath:' + goFilePath
def methodName = ''
File file = new File(goFilePath)
def lineNum = 0
def funcLine = ''
def isBreak = falseL
file.eachLine { line ->
            if (!isBreak) {
        lineNum++
        // println line.indexOf('func ')  + ':' + lineNum + ':' + line
        if (line.indexOf('func ') == 0 ) {
            funcLine = line
        // println 'funcLine=' + funcLine
        }
        isBreak = lineNum >= tagLineNum
            // println 'isBreak=' + isBreak
            }
// if (isBreak)  {// TODO 这里应该返回
// }
}
        // println 'result:' + funcLine

funcLine = funcLine.substring(4)
        // println 'result1:' + funcLine

funcLine = funcLine.replaceAll("\\s", '')
        // println 'result2:' + funcLine

def end = funcLine.indexOf('(')
// println 'end:' + end
if (end < 0) {
    return methodName
}

if (end == 0) {
    def tmp = funcLine.indexOf(')')
    if (tmp >= 0) {
        funcLine = funcLine.substring(tmp + 1)
                // println 'result23' + funcLine

        end = funcLine.indexOf('(')
    // println 'end:' + end
    }
}
if (end <= 0) {
    return methodName
}

methodName = funcLine.substring(0, end)


return methodName
