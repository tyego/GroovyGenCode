class StructStringTest {

    static String out(String log) {
        println log
        return true
    }

    static String getMethodName(String goFilePath, int tagLineNum) {
        // 获取最接近tagLineNum的struct的开始行号，以及name
        out('goFilePath:' + goFilePath)
        File file = new File(goFilePath)

        def lineNum = 0
        def structName = ''

        def params = new ArrayList<String>()
        def readingParams = false

        def isBreak = false // 没找到eachLine的break方法，暂时这么写
        file.eachLine { line ->
            if (isBreak) { // 即便返回false，这里还是会继续执行
                return false
            }

            lineNum++
            def typeIndex = line.indexOf('type ')
            out('lineNum = ' + lineNum + ', typeIndex = ' + typeIndex)
            if (typeIndex == 0) {
                def splits = line.split('\\s+')
                out('len=' + splits.length + ', splits = ' + splits)

                if (splits.length == 4 && 'struct' == splits[2] && '{' == splits[3]) {
                    structName = splits[1]
                    params.clear()
                    readingParams = true
                    out('item structName =' + splits[1])
                }
            } else if (readingParams) {
                def splits = line.split('\\s+')
                out('splits length =' + splits.length)
                if (splits.length == 1 && splits[0] == '}') {
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
        out('structName=' + structName + ', params = ' + params)

        def sb = new StringBuffer()
        sb.append('\nfunc (i *')
        sb.append(structName)
        sb.append(') String() string {\n')
        sb.append('\tb := strings.Builder{}\n')
        sb.append('\tif i == nil {\n')
        sb.append('\t\treturn "nil"\n')
        sb.append('\t}\n')

        sb.append('\tb.WriteString("')
        sb.append(structName)
        sb.append('{")\n')

        def firstParam = true
        for (int i = 0; i < params.size(); i++) {
            def param = params.get(i).trim()
            out('param = ' + param)

            def splits = param.split('\\s+')
            out('splits=' + splits + ', len = ' + splits.length)
            if (splits.length < 2) {
                continue
            }

            // 如果参数后的双斜杠注释中，首部或尾部添加@s或@i标记，则该参数会调用String()方法或转int输出
            def printPointStr = false
            def printStr = false
            def printInt = false
            def printLen = false
            def lineSplit = param.split("//")
            if (lineSplit.length > 1) {
                param = lineSplit[0]
                def commentSplit = lineSplit[1].trim().split('\\s+')
                if (commentSplit.length > 0) {
                    if (commentSplit[0] == '@ignore' || commentSplit[commentSplit.length - 1] == '@ignore') {
                        continue
                    } else if (commentSplit[0] == '@.s' || commentSplit[commentSplit.length - 1] == '@.s') {
                        printPointStr = true
                    } else if (commentSplit[0] == '@s' || commentSplit[commentSplit.length - 1] == '@s') {
                        printStr = true
                    } else if (commentSplit[0] == '@i' || commentSplit[commentSplit.length - 1] == '@i') {
                        printInt = true
                    } else if (commentSplit[0] == '@l' || commentSplit[commentSplit.length - 1] == '@l') {
                        printLen = true
                    }
                }
                // 重新切割
                splits = param.split('\\s+')
                out('splits=' + splits + ', len = ' + splits.length)
            }

            if (splits.length == 2 || (splits.length > 2 && splits[2].indexOf('`') == 0)) {
                if (firstParam) {
                    sb.append('\tb.WriteString("')
                    firstParam = false
                } else {
                    sb.append('\tb.WriteString(", ')
                }
                sb.append(splits[0])
                if (printLen) { // 如果是打印长度，则变量名后加标记
                    sb.append('.len')
                }
                sb.append(':")\n')

                // 处理特殊标记
                if (printPointStr) {
                    sb.append('\tb.WriteString(i.')
                    sb.append(splits[0])
                    sb.append('.String())\n')
                    continue
                }
                if (printStr) {
                    sb.append('\tb.WriteString(string(i.')
                    sb.append(splits[0])
                    sb.append('))\n')
                    continue
                }
                if (printInt) {
                    sb.append('\tb.WriteString(strconv.Itoa(int(i.')
                    sb.append(splits[0])
                    sb.append(')))\n')
                    continue
                }
                if (printLen) {
                    sb.append('\tb.WriteString(strconv.Itoa(len(i.')
                    sb.append(splits[0])
                    sb.append(')))\n')
                    continue
                }

                // 处理标准格式
                switch (splits[1]) {
                    case 'string':
                        sb.append('\tb.WriteString(i.')
                        sb.append(splits[0])
                        sb.append(')\n')
                        break
                    case 'int':
                        sb.append('\tb.WriteString(strconv.Itoa(i.')
                        sb.append(splits[0])
                        sb.append('))\n')
                        break
                    case 'int32':
                        sb.append('\tb.WriteString(strconv.Itoa(int(i.')
                        sb.append(splits[0])
                        sb.append(')))\n')
                        break
                    case 'int64':
                        sb.append('\tb.WriteString(strconv.FormatInt(i.')
                        sb.append(splits[0])
                        sb.append(', 10))\n')
                        break
                    case 'uint8':
                        sb.append('\tb.WriteString(strconv.Itoa(int(i.')
                        sb.append(splits[0])
                        sb.append(')))\n')
                        break
                    case 'uint16':
                        sb.append('\tb.WriteString(strconv.Itoa(int(i.')
                        sb.append(splits[0])
                        sb.append(')))\n')
                        break
                    case 'uint32':
                        sb.append('\tb.WriteString(strconv.FormatUint(uint64(i.')
                        sb.append(splits[0])
                        sb.append('), 10))\n')
                        break
                    case 'uint64':
                        sb.append('\tb.WriteString(strconv.FormatUint(i.')
                        sb.append(splits[0])
                        sb.append(', 10))\n')
                        break
                    case 'bool':
                        sb.append('\tb.WriteString(strconv.FormatBool(i.')
                        sb.append(splits[0])
                        sb.append('))\n')
                        break
                    default:
                        sb.append('\tb.WriteString(fmt.Sprintf("%+v", i.')
                        sb.append(splits[0])
                        sb.append('))\n')
                        break
                }
            }
        }
        sb.append('\tb.WriteByte(\'}\')\n')
        sb.append('\treturn b.String()\n')
        sb.append('}\n')
        def result = sb.toString()
        return result
    }

    static void main(String[] args) {
        def goFilePath = '/Users/tyewang/go/src/git.wecomm.link/server/call/data/mysql.go'
        def tagLineNum = 232

        def result = getMethodName(goFilePath, tagLineNum)
        println 'result=' + result
    }

}