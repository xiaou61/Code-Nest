/**
 * OJ Monaco 编辑器智能补全 + 自动导入 + 点号补全
 */

let disposables = []

/**
 * 注册所有语言的补全提供器
 */
export function registerCompletionProviders(monaco) {
  disposables.forEach(d => d.dispose())
  disposables = []

  disposables.push(
    // Java - 关键字/类补全
    monaco.languages.registerCompletionItemProvider('java', {
      provideCompletionItems: (model, position) => ({
        suggestions: getJavaSuggestions(monaco, model, position)
      })
    }),
    // Java - 点号触发的方法补全
    monaco.languages.registerCompletionItemProvider('java', {
      triggerCharacters: ['.'],
      provideCompletionItems: (model, position) => ({
        suggestions: getJavaDotSuggestions(monaco, model, position)
      })
    }),
    monaco.languages.registerCompletionItemProvider('cpp', {
      triggerCharacters: ['#', '<'],
      provideCompletionItems: (model, position) => ({
        suggestions: getCppSuggestions(monaco, model, position)
      })
    }),
    // C++ - 点号触发的方法补全
    monaco.languages.registerCompletionItemProvider('cpp', {
      triggerCharacters: ['.'],
      provideCompletionItems: (model, position) => ({
        suggestions: getCppDotSuggestions(monaco, model, position)
      })
    }),
    monaco.languages.registerCompletionItemProvider('c', {
      triggerCharacters: ['#', '<'],
      provideCompletionItems: (model, position) => ({
        suggestions: getCppSuggestions(monaco, model, position)
      })
    }),
    monaco.languages.registerCompletionItemProvider('python', {
      provideCompletionItems: (model, position) => ({
        suggestions: getPythonSuggestions(monaco, model, position)
      })
    }),
    // Python - 点号触发的方法补全
    monaco.languages.registerCompletionItemProvider('python', {
      triggerCharacters: ['.'],
      provideCompletionItems: (model, position) => ({
        suggestions: getPythonDotSuggestions(monaco, model, position)
      })
    }),
    monaco.languages.registerCompletionItemProvider('go', {
      provideCompletionItems: (model, position) => ({
        suggestions: getGoSuggestions(monaco, model, position)
      })
    }),
    monaco.languages.registerCompletionItemProvider('javascript', {
      provideCompletionItems: (model, position) => ({
        suggestions: getJsSuggestions(monaco, model, position)
      })
    })
  )
}

export function disposeCompletionProviders() {
  disposables.forEach(d => d.dispose())
  disposables = []
}

// ============ 工具函数 ============

function wordRange(model, position) {
  const word = model.getWordUntilPosition(position)
  return {
    startLineNumber: position.lineNumber,
    endLineNumber: position.lineNumber,
    startColumn: word.startColumn,
    endColumn: word.endColumn
  }
}

function findImportInsertLine(lines, importPrefix, classPattern) {
  let insertLine = 0
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].startsWith(importPrefix)) {
      insertLine = i + 1
    } else if (classPattern && classPattern.test(lines[i])) {
      if (insertLine === 0) insertLine = i
      break
    }
  }
  return insertLine
}

function makeAutoImportEdit(monaco, code, importStatement, importPrefix, classPattern) {
  if (code.includes(importStatement)) return []
  const lines = code.split('\n')
  const insertLine = findImportInsertLine(lines, importPrefix, classPattern)
  return [{
    range: new monaco.Range(insertLine + 1, 1, insertLine + 1, 1),
    text: importStatement + '\n'
  }]
}

// ============ Java ============

const JAVA_CLASSES = [
  { name: 'Scanner', pkg: 'java.util.Scanner' },
  { name: 'ArrayList', pkg: 'java.util.ArrayList', snippet: 'ArrayList<${1:Integer}>' },
  { name: 'LinkedList', pkg: 'java.util.LinkedList', snippet: 'LinkedList<${1:Integer}>' },
  { name: 'HashMap', pkg: 'java.util.HashMap', snippet: 'HashMap<${1:String}, ${2:Integer}>' },
  { name: 'HashSet', pkg: 'java.util.HashSet', snippet: 'HashSet<${1:Integer}>' },
  { name: 'TreeMap', pkg: 'java.util.TreeMap', snippet: 'TreeMap<${1:Integer}, ${2:Integer}>' },
  { name: 'TreeSet', pkg: 'java.util.TreeSet', snippet: 'TreeSet<${1:Integer}>' },
  { name: 'PriorityQueue', pkg: 'java.util.PriorityQueue', snippet: 'PriorityQueue<${1:Integer}>' },
  { name: 'Queue', pkg: 'java.util.Queue', snippet: 'Queue<${1:Integer}>' },
  { name: 'Deque', pkg: 'java.util.Deque', snippet: 'Deque<${1:Integer}>' },
  { name: 'Stack', pkg: 'java.util.Stack', snippet: 'Stack<${1:Integer}>' },
  { name: 'Arrays', pkg: 'java.util.Arrays' },
  { name: 'Collections', pkg: 'java.util.Collections' },
  { name: 'StringBuilder', pkg: null },
  { name: 'BufferedReader', pkg: 'java.io.BufferedReader' },
  { name: 'InputStreamReader', pkg: 'java.io.InputStreamReader' },
  { name: 'StringTokenizer', pkg: 'java.util.StringTokenizer' },
  { name: 'Map', pkg: 'java.util.Map', snippet: 'Map<${1:String}, ${2:Integer}>' },
  { name: 'List', pkg: 'java.util.List', snippet: 'List<${1:Integer}>' },
  { name: 'Set', pkg: 'java.util.Set', snippet: 'Set<${1:Integer}>' },
]

const JAVA_SNIPPETS = [
  { label: 'sout', text: 'System.out.println(${1});', detail: 'System.out.println()' },
  { label: 'soutf', text: 'System.out.printf("${1}\\n"${2});', detail: 'System.out.printf()' },
  { label: 'fori', text: 'for (int ${1:i} = 0; ${1:i} < ${2:n}; ${1:i}++) {\n\t${0}\n}', detail: 'for-i 循环' },
  { label: 'fore', text: 'for (${1:int} ${2:x} : ${3:arr}) {\n\t${0}\n}', detail: 'for-each 循环' },
  { label: 'whsc', text: 'while (sc.hasNext()) {\n\t${0}\n}', detail: 'while Scanner' },
  { label: 'scni', text: 'int ${1:n} = sc.nextInt();', detail: 'sc.nextInt()' },
  { label: 'scnl', text: 'String ${1:s} = sc.nextLine();', detail: 'sc.nextLine()' },
  { label: 'scnd', text: 'double ${1:d} = sc.nextDouble();', detail: 'sc.nextDouble()' },
  { label: 'arr', text: 'int[] ${1:arr} = new int[${2:n}];', detail: 'int 数组' },
  { label: 'arr2d', text: 'int[][] ${1:grid} = new int[${2:m}][${3:n}];', detail: '二维数组' },
  { label: 'br', text: 'BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\nStringTokenizer st;', detail: '快速读入' },
]

function getJavaSuggestions(monaco, model, position) {
  const range = wordRange(model, position)
  const code = model.getValue()
  const suggestions = []
  const Kind = monaco.languages.CompletionItemKind
  const SnippetRule = monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet

  for (const cls of JAVA_CLASSES) {
    const additionalTextEdits = cls.pkg
      ? makeAutoImportEdit(monaco, code, `import ${cls.pkg};`, 'import ', /^(public\s+)?class\s/)
      : []
    suggestions.push({
      label: cls.name,
      kind: Kind.Class,
      insertText: cls.snippet || cls.name,
      insertTextRules: cls.snippet ? SnippetRule : undefined,
      range,
      additionalTextEdits,
      detail: cls.pkg || 'java.lang',
      sortText: '0_' + cls.name
    })
  }

  for (const s of JAVA_SNIPPETS) {
    suggestions.push({
      label: s.label,
      kind: Kind.Snippet,
      insertText: s.text,
      insertTextRules: SnippetRule,
      range,
      detail: s.detail,
      sortText: '1_' + s.label
    })
  }

  return suggestions
}

function getJavaDotSuggestions(monaco, model, position) {
  const range = wordRange(model, position)
  const Kind = monaco.languages.CompletionItemKind
  return [
    { label: 'length()', insertText: 'length()', detail: 'String.length()', kind: Kind.Method, range },
    { label: 'charAt()', insertText: 'charAt(${1:index})', detail: 'String.charAt(index)', kind: Kind.Method, range },
    { label: 'substring()', insertText: 'substring(${1:start}, ${2:end})', detail: 'String.substring(start, end)', kind: Kind.Method, range },
    { label: 'size()', insertText: 'size()', detail: 'Collection.size()', kind: Kind.Method, range },
    { label: 'get()', insertText: 'get(${1:index})', detail: 'List.get(index)', kind: Kind.Method, range },
    { label: 'add()', insertText: 'add(${1:value})', detail: 'Collection.add(value)', kind: Kind.Method, range }
  ].map(item => ({
    ...item,
    insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
  }))
}

// ============ C++ ============

const CPP_CLASSES = [
  { name: 'vector', header: '<vector>', snippet: 'vector<${1:int}>' },
  { name: 'map', header: '<map>', snippet: 'map<${1:string}, ${2:int}>' },
  { name: 'set', header: '<set>', snippet: 'set<${1:int}>' },
  { name: 'unordered_map', header: '<unordered_map>', snippet: 'unordered_map<${1:string}, ${2:int}>' },
  { name: 'unordered_set', header: '<unordered_set>', snippet: 'unordered_set<${1:int}>' },
  { name: 'queue', header: '<queue>', snippet: 'queue<${1:int}>' },
  { name: 'priority_queue', header: '<queue>', snippet: 'priority_queue<${1:int}>' },
  { name: 'stack', header: '<stack>', snippet: 'stack<${1:int}>' },
  { name: 'deque', header: '<deque>', snippet: 'deque<${1:int}>' },
  { name: 'string', header: '<string>' },
  { name: 'pair', header: '<utility>', snippet: 'pair<${1:int}, ${2:int}>' },
  { name: 'bitset', header: '<bitset>', snippet: 'bitset<${1:32}>' },
]

const CPP_SNIPPETS = [
  { label: 'cout', text: 'cout << ${1} << endl;', detail: 'cout 输出' },
  { label: 'cin', text: 'cin >> ${1};', detail: 'cin 读入' },
  { label: 'fori', text: 'for (int ${1:i} = 0; ${1:i} < ${2:n}; ${1:i}++) {\n\t${0}\n}', detail: 'for-i 循环' },
  { label: 'fore', text: 'for (auto& ${1:x} : ${2:v}) {\n\t${0}\n}', detail: 'range-for 循环' },
  { label: 'sort', text: 'sort(${1:v}.begin(), ${1:v}.end());', detail: 'sort 排序' },
  { label: 'reverse', text: 'reverse(${1:v}.begin(), ${1:v}.end());', detail: 'reverse 翻转' },
  { label: 'pb', text: '${1:v}.push_back(${2});', detail: 'push_back' },
  { label: 'lb', text: 'lower_bound(${1:v}.begin(), ${1:v}.end(), ${2:val})', detail: 'lower_bound' },
  { label: 'ub', text: 'upper_bound(${1:v}.begin(), ${1:v}.end(), ${2:val})', detail: 'upper_bound' },
  { label: 'acc', text: 'accumulate(${1:v}.begin(), ${1:v}.end(), ${2:0})', detail: 'accumulate' },
  { label: 'bits', text: '#include <bits/stdc++.h>', detail: '万能头文件' },
]

function getCppSuggestions(monaco, model, position) {
  const range = wordRange(model, position)
  const code = model.getValue()
  const suggestions = []
  const Kind = monaco.languages.CompletionItemKind
  const SnippetRule = monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet

  for (const cls of CPP_CLASSES) {
    const include = `#include ${cls.header}`
    const additionalTextEdits = makeAutoImportEdit(monaco, code, include, '#include', null)
    suggestions.push({
      label: cls.name,
      kind: Kind.Class,
      insertText: cls.snippet || cls.name,
      insertTextRules: cls.snippet ? SnippetRule : undefined,
      range,
      additionalTextEdits,
      detail: include,
      sortText: '0_' + cls.name
    })
  }

  for (const s of CPP_SNIPPETS) {
    suggestions.push({
      label: s.label,
      kind: Kind.Snippet,
      insertText: s.text,
      insertTextRules: SnippetRule,
      range,
      detail: s.detail,
      sortText: '1_' + s.label
    })
  }

  return suggestions
}

function getCppDotSuggestions(monaco, model, position) {
  const range = wordRange(model, position)
  const Kind = monaco.languages.CompletionItemKind
  return [
    { label: 'size()', insertText: 'size()', detail: 'container.size()', kind: Kind.Method, range },
    { label: 'empty()', insertText: 'empty()', detail: 'container.empty()', kind: Kind.Method, range },
    { label: 'push_back()', insertText: 'push_back(${1:value})', detail: 'vector.push_back(value)', kind: Kind.Method, range },
    { label: 'pop_back()', insertText: 'pop_back()', detail: 'vector.pop_back()', kind: Kind.Method, range },
    { label: 'begin()', insertText: 'begin()', detail: 'container.begin()', kind: Kind.Method, range },
    { label: 'end()', insertText: 'end()', detail: 'container.end()', kind: Kind.Method, range }
  ].map(item => ({
    ...item,
    insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
  }))
}

// ============ Python ============

const PYTHON_SNIPPETS = [
  { label: 'inp', text: '${1:n} = int(input())', detail: '读整数' },
  { label: 'inps', text: '${1:a}, ${2:b} = map(int, input().split())', detail: '读多个整数' },
  { label: 'inpl', text: '${1:arr} = list(map(int, input().split()))', detail: '读整数列表' },
  { label: 'inpstr', text: '${1:s} = input().strip()', detail: '读字符串' },
  { label: 'print', text: 'print(${1})', detail: 'print()' },
  { label: 'fori', text: 'for ${1:i} in range(${2:n}):\n\t${0}', detail: 'for-range 循环' },
  { label: 'fore', text: 'for ${1:x} in ${2:arr}:\n\t${0}', detail: 'for-in 循环' },
  { label: 'enu', text: 'for ${1:i}, ${2:x} in enumerate(${3:arr}):\n\t${0}', detail: 'enumerate 循环' },
  { label: 'defaultdict', text: 'from collections import defaultdict\n${1:d} = defaultdict(${2:int})', detail: 'defaultdict' },
  { label: 'Counter', text: 'from collections import Counter\n${1:c} = Counter(${2:arr})', detail: 'Counter' },
  { label: 'deque', text: 'from collections import deque\n${1:q} = deque()', detail: 'deque 双端队列' },
  { label: 'heapq', text: 'import heapq\nheapq.heappush(${1:heap}, ${2:val})', detail: 'heapq 堆' },
  { label: 'bisect', text: 'from bisect import bisect_left, bisect_right', detail: 'bisect 二分' },
  { label: 'inf', text: 'float("inf")', detail: '正无穷' },
  { label: 'lc', text: '[${1:x} for ${1:x} in ${2:arr}${3: if ${4:condition}}]', detail: '列表推导' },
  { label: 'sys', text: 'import sys\ninput = sys.stdin.readline', detail: '快速读入' },
]

function getPythonSuggestions(monaco, model, position) {
  const range = wordRange(model, position)
  return PYTHON_SNIPPETS.map(s => ({
    label: s.label,
    kind: monaco.languages.CompletionItemKind.Snippet,
    insertText: s.text,
    insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    range,
    detail: s.detail,
  }))
}

function getPythonDotSuggestions(monaco, model, position) {
  const range = wordRange(model, position)
  const Kind = monaco.languages.CompletionItemKind
  return [
    { label: 'append()', insertText: 'append(${1:value})', detail: 'list.append(value)', kind: Kind.Method, range },
    { label: 'pop()', insertText: 'pop()', detail: 'list.pop()', kind: Kind.Method, range },
    { label: 'sort()', insertText: 'sort()', detail: 'list.sort()', kind: Kind.Method, range },
    { label: 'split()', insertText: 'split(${1})', detail: 'str.split()', kind: Kind.Method, range },
    { label: 'strip()', insertText: 'strip()', detail: 'str.strip()', kind: Kind.Method, range },
    { label: 'items()', insertText: 'items()', detail: 'dict.items()', kind: Kind.Method, range }
  ].map(item => ({
    ...item,
    insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
  }))
}

// ============ Go ============

const GO_SNIPPETS = [
  { label: 'fmtp', text: 'fmt.Println(${1})', detail: 'fmt.Println()' },
  { label: 'fmtf', text: 'fmt.Printf("${1}\\n"${2})', detail: 'fmt.Printf()' },
  { label: 'fmts', text: 'fmt.Scan(&${1})', detail: 'fmt.Scan()' },
  { label: 'fori', text: 'for ${1:i} := 0; ${1:i} < ${2:n}; ${1:i}++ {\n\t${0}\n}', detail: 'for-i 循环' },
  { label: 'fore', text: 'for ${1:i}, ${2:v} := range ${3:arr} {\n\t${0}\n}', detail: 'for-range 循环' },
  { label: 'makeslice', text: '${1:s} := make([]${2:int}, ${3:n})', detail: 'make slice' },
  { label: 'makemap', text: '${1:m} := make(map[${2:string}]${3:int})', detail: 'make map' },
  { label: 'sort', text: 'sort.Ints(${1:arr})', detail: 'sort.Ints()' },
]

function getGoSuggestions(monaco, model, position) {
  const range = wordRange(model, position)
  return GO_SNIPPETS.map(s => ({
    label: s.label,
    kind: monaco.languages.CompletionItemKind.Snippet,
    insertText: s.text,
    insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    range,
    detail: s.detail,
  }))
}

// ============ JavaScript ============

const JS_SNIPPETS = [
  { label: 'cl', text: 'console.log(${1});', detail: 'console.log()' },
  { label: 'rl', text: "const readline = require('readline');\nconst rl = readline.createInterface({ input: process.stdin });\nconst lines = [];\nrl.on('line', l => lines.push(l));\nrl.on('close', () => {\n\t${0}\n});", detail: 'readline 模板' },
  { label: 'fori', text: 'for (let ${1:i} = 0; ${1:i} < ${2:n}; ${1:i}++) {\n\t${0}\n}', detail: 'for-i 循环' },
  { label: 'fore', text: 'for (const ${1:x} of ${2:arr}) {\n\t${0}\n}', detail: 'for-of 循环' },
  { label: 'map', text: '${1:arr}.map(${2:x} => ${3:x})', detail: '.map()' },
  { label: 'filter', text: '${1:arr}.filter(${2:x} => ${3:condition})', detail: '.filter()' },
  { label: 'reduce', text: '${1:arr}.reduce((${2:acc}, ${3:x}) => ${4:acc + x}, ${5:0})', detail: '.reduce()' },
  { label: 'sort', text: '${1:arr}.sort((a, b) => a - b)', detail: '.sort() 升序' },
]

function getJsSuggestions(monaco, model, position) {
  const range = wordRange(model, position)
  return JS_SNIPPETS.map(s => ({
    label: s.label,
    kind: monaco.languages.CompletionItemKind.Snippet,
    insertText: s.text,
    insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    range,
    detail: s.detail,
  }))
}
