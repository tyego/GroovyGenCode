class EnumToString {

    static void main(String[] args) {
        def goFilePath = '/Users/tyewang/go/src/git.wecomm.link/server/pylon/business/packet.go'
        def tagLineNum = 160

        def result = getMethodName(goFilePath, tagLineNum)
        println 'result=' + result
    }

    static String out(String log) {
        println log
        return true
    }

    static String getMethodName(String goFilePath, int tagLineNum) {
        // 获取最接近tagLineNum的struct的开始行号，以及name
        out('goFilePath:' + goFilePath)
        File file = new File(goFilePath)

        def lineNum = 0
        def enumName = ''

        def params = new ArrayList<String>()
        def readingParams = false

        def isBreak = false // 没找到eachLine的break方法，暂时这么写
        file.eachLine { line ->
            if (isBreak) { // 即便返回false，这里还是会继续执行
                return false
            }

            lineNum++
            def typeIndex = line.indexOf('const (')
            out('lineNum = ' + lineNum + ', typeIndex = ' + typeIndex)
            if (typeIndex == 0) {
                enumName = ''
                params.clear()
                readingParams = true
                out('reading enum')
            } else if (readingParams) {
                def splits = line.trim().split('\\s+')
                out('splits length =' + splits.length + ', splits = ' + splits)

                if (enumName == '' && splits.length >= 4 && splits[2] == '=') {
                    enumName = splits[1]
                }
                if (splits.length == 1 && splits[0] == ')') {
                    readingParams = false
                } else {
                    params.add(line)
                }
            }
            if (lineNum >= tagLineNum && !readingParams) {
                isBreak = true
            }
            return true
        }
        out('enumName=' + enumName + ', params = ' + params)

        def sb = new StringBuffer()
        sb.append('\nfunc (i ').append(enumName).append(') String() string {\n')
        sb.append('\tswitch i {\n')

        for (int i = 0; i < params.size(); i++) {
            def param = params.get(i).trim()
            out('param = ' + param)

            // 去掉注释
            def lineSplit = param.split("//")
            if (lineSplit.length > 1) {
                param = lineSplit[0]
            }

            def splits = param.split('\\s+')
            out('splits=' + splits + ', len = ' + splits.length)
            if (splits.length != 1 && splits.length != 4) {
                continue
            }

            sb.append('\tcase ').append(splits[0]).append(':\n')
            sb.append('\t\treturn "').append(splits[0]).append('"\n')
        }

        sb.append('\tdefault:\n')
        sb.append('\t\treturn strconv.Itoa(int(i))\n')
        sb.append('\t}\n')

        sb.append('}\n')
        def result = sb.toString()
        return result
    }

}