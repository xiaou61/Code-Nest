# AC 自动机（Aho-Corasick）算法技术文档

## 一、什么是 AC 自动机？

### 1.1 问题背景

假设你要在一篇文章中查找是否包含多个敏感词：

```
文章：ushers（6个字符）
敏感词库：he, she, his, hers（4个词）
```

**暴力做法**：用每个敏感词去文章里逐个匹配
- 时间复杂度：O(n × m × k)，n=文章长度，m=敏感词数量，k=敏感词平均长度
- 词库有 5 万个词时，每篇文章要匹配 5 万次，太慢了

**AC 自动机**：只扫描文章一遍，同时匹配所有敏感词
- 时间复杂度：**O(n)**，与敏感词数量无关！

### 1.2 算法定义

> **AC 自动机 = Trie 树 + Failure 指针（失败指针）**

- **Trie 树**：把所有敏感词组织成一棵树
- **Failure 指针**：匹配失败时，告诉你该跳到哪里继续匹配（类似 KMP 算法的 next 数组）

---

## 二、Trie 树（前缀树）

### 2.1 什么是 Trie 树

Trie 树是一种树形结构，用于高效存储和查找字符串集合。

**特点**：
- 根节点不包含字符
- 每条边代表一个字符
- 从根到某个节点的路径，就是一个字符串的前缀
- 标记为"结束"的节点，表示一个完整的单词

### 2.2 图示

敏感词：`he`, `she`, `his`, `hers`

```
            root
           /    \
          h      s
         / \      \
        e*  i      h
        |   |      |
        r   s*     e*
        |          |
        s*         r
                   |
                   s*

* 表示这是一个敏感词的结尾
```

**路径解读**：
- root → h → e（he✓）
- root → h → e → r → s（hers✓）
- root → h → i → s（his✓）
- root → s → h → e（she✓）

### 2.3 Trie 树代码实现

```java
/**
 * Trie 树节点
 */
class TrieNode {
    // 子节点映射：字符 -> 子节点
    Map<Character, TrieNode> children = new HashMap<>();
    
    // 失败指针（AC 自动机的核心）
    TrieNode failure;
    
    // 是否是某个敏感词的结尾
    boolean isEndOfWord = false;
    
    // 如果是结尾，存储完整的敏感词
    String pattern;
}
```

---

## 三、Failure 指针（失败指针）

### 3.1 为什么需要 Failure 指针？

假设我们用 Trie 树匹配文本 `ushers`：

```
文本：u s h e r s
      ↑
      从 u 开始，root 没有 u 这个子节点，匹配失败
      
      回到 root，从 s 开始...
```

**问题**：每次失败都回到 root 重新开始，效率不高。

**Failure 指针的作用**：失败时不回到 root，而是跳到"最长后缀匹配"的位置。

### 3.2 什么是"最长后缀匹配"？

```
当前已匹配：s → h → e（匹配了 "she"）
下一个字符是 r，但 she 节点没有 r 这个子节点

此时 "she" 的后缀有：
- "he"（2个字符）← 这个在 Trie 中存在！
- "e"（1个字符）

所以 she 的 failure 指针指向 he 节点
这样就能继续匹配 "hers"
```

### 3.3 图示 Failure 指针

```
            root ←─────────────────────┐
           /    \                      │
          h      s                     │
         / \      \                    │
        e*  i      h ─── failure ───→ h
        |   |      |                   │
        r   s*     e* ── failure ──→ e*
        |          |                   │
        s*         r ─── failure ───→ r
                   |                   │
                   s* ── failure ──→ s*

failure 指针总是指向"当前路径的最长真后缀"在 Trie 中的位置
```

### 3.4 Failure 指针的构建规则

1. **根节点的所有直接子节点**：failure 指向 root
2. **其他节点**：
   - 假设当前节点是 `current`，它的父节点是 `parent`，`parent` 到 `current` 的边是字符 `c`
   - 从 `parent.failure` 开始，看它有没有字符 `c` 的子节点
   - 如果有，`current.failure = parent.failure.children[c]`
   - 如果没有，继续沿着 `parent.failure.failure` 找，直到找到或回到 root

---

## 四、完整算法流程

### 4.1 构建阶段（预处理）

```
输入：敏感词列表 ["he", "she", "his", "hers"]
输出：带有 Failure 指针的 AC 自动机

步骤：
1. 构建 Trie 树（时间复杂度：O(所有敏感词总长度)）
2. BFS 构建 Failure 指针（时间复杂度：O(所有敏感词总长度)）
```

### 4.2 匹配阶段

```
输入：待检测文本 "ushers"
输出：命中的敏感词列表 ["she", "he", "hers"]

步骤：
1. 从 root 开始，逐个字符扫描文本
2. 如果当前节点有这个字符的子节点，移动到子节点
3. 如果没有，沿着 failure 指针跳转，直到找到或回到 root
4. 每到一个节点，检查它及其 failure 链上是否有敏感词结尾
5. 只扫描一遍，时间复杂度 O(n)
```

### 4.3 匹配过程图示

```
文本：u s h e r s
     0 1 2 3 4 5

位置0：u
  - root 没有 u 子节点，停在 root

位置1：s
  - root 有 s 子节点，移动到 s

位置2：h
  - s 有 h 子节点，移动到 sh

位置3：e
  - sh 有 e 子节点，移动到 she
  - she 是敏感词结尾！命中 "she"
  - 检查 failure 链：she.failure = he，he 也是结尾！命中 "he"

位置4：r
  - she 没有 r 子节点
  - 跳转到 she.failure = he
  - he 有 r 子节点，移动到 her

位置5：s
  - her 有 s 子节点，移动到 hers
  - hers 是敏感词结尾！命中 "hers"

结果：["she", "he", "hers"]
```

---

## 五、完整 Java 代码实现

### 5.1 AC 自动机引擎

```java
package com.xiaou.sensitive.engine;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * AC 自动机敏感词检测引擎
 * 
 * 算法原理：
 * 1. 将所有敏感词构建成 Trie 树
 * 2. 通过 BFS 构建 Failure 指针
 * 3. 扫描文本时，利用 Failure 指针实现 O(n) 匹配
 * 
 * @author xiaou
 */
public class AhoCorasickEngine {

    /** Trie 树根节点 */
    private TrieNode root;
    
    /** 读写锁：保证词库更新时的并发安全 */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Trie 树节点
     */
    private static class TrieNode {
        /** 子节点映射 */
        Map<Character, TrieNode> children = new HashMap<>();
        
        /** 失败指针 */
        TrieNode failure;
        
        /** 是否是敏感词结尾 */
        boolean isEnd = false;
        
        /** 完整的敏感词（只在结尾节点存储） */
        String pattern;
    }

    /**
     * 初始化 AC 自动机
     * 
     * @param words 敏感词集合
     */
    public void initialize(Set<String> words) {
        lock.writeLock().lock();
        try {
            // 1. 构建 Trie 树
            buildTrie(words);
            // 2. 构建 Failure 指针
            buildFailurePointers();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 第一步：构建 Trie 树
     */
    private void buildTrie(Set<String> words) {
        root = new TrieNode();

        for (String word : words) {
            if (word == null || word.isEmpty()) {
                continue;
            }

            TrieNode current = root;
            
            // 逐字符插入
            for (char c : word.toLowerCase().toCharArray()) {
                if (!current.children.containsKey(c)) {
                    current.children.put(c, new TrieNode());
                }
                current = current.children.get(c);
            }
            
            // 标记敏感词结尾
            current.isEnd = true;
            current.pattern = word.toLowerCase();
        }
    }

    /**
     * 第二步：BFS 构建 Failure 指针
     */
    private void buildFailurePointers() {
        Queue<TrieNode> queue = new LinkedList<>();

        // 第一层：所有直接子节点的 failure 指向 root
        for (TrieNode child : root.children.values()) {
            child.failure = root;
            queue.offer(child);
        }

        // BFS 遍历其余节点
        while (!queue.isEmpty()) {
            TrieNode current = queue.poll();

            for (Map.Entry<Character, TrieNode> entry : current.children.entrySet()) {
                char c = entry.getKey();
                TrieNode child = entry.getValue();
                queue.offer(child);

                // 寻找 failure 指针
                TrieNode failure = current.failure;
                
                // 沿着父节点的 failure 链向上找
                while (failure != null && !failure.children.containsKey(c)) {
                    failure = failure.failure;
                }

                // 设置 failure 指针
                if (failure == null) {
                    child.failure = root;
                } else {
                    child.failure = failure.children.get(c);
                }

                // 避免自环
                if (child.failure == child) {
                    child.failure = root;
                }
            }
        }
    }

    /**
     * 检测文本中的敏感词
     * 
     * @param text 待检测文本
     * @return 命中的敏感词列表
     */
    public List<String> findSensitiveWords(String text) {
        if (text == null || text.isEmpty() || root == null) {
            return new ArrayList<>();
        }

        lock.readLock().lock();
        try {
            Set<String> result = new HashSet<>();
            TrieNode current = root;
            String lowerText = text.toLowerCase();

            // 只扫描一遍：O(n)
            for (int i = 0; i < lowerText.length(); i++) {
                char c = lowerText.charAt(i);

                // 如果当前节点没有这个字符，沿 failure 指针跳转
                while (current != root && !current.children.containsKey(c)) {
                    current = current.failure;
                }

                // 如果有这个字符，移动到子节点
                if (current.children.containsKey(c)) {
                    current = current.children.get(c);
                }

                // 检查当前节点及 failure 链上的所有敏感词
                TrieNode temp = current;
                while (temp != null && temp != root) {
                    if (temp.isEnd) {
                        result.add(temp.pattern);
                    }
                    temp = temp.failure;
                }
            }

            return new ArrayList<>(result);
            
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 替换敏感词
     * 
     * @param text 原文本
     * @param replacement 替换字符（如 "*"）
     * @return 替换后的文本
     */
    public String replaceSensitiveWords(String text, String replacement) {
        if (text == null || text.isEmpty() || root == null) {
            return text;
        }

        lock.readLock().lock();
        try {
            // 记录所有匹配位置
            List<int[]> matches = new ArrayList<>();
            TrieNode current = root;
            String lowerText = text.toLowerCase();

            for (int i = 0; i < lowerText.length(); i++) {
                char c = lowerText.charAt(i);

                while (current != root && !current.children.containsKey(c)) {
                    current = current.failure;
                }

                if (current.children.containsKey(c)) {
                    current = current.children.get(c);
                }

                TrieNode temp = current;
                while (temp != null && temp != root) {
                    if (temp.isEnd) {
                        int start = i - temp.pattern.length() + 1;
                        int end = i + 1;
                        matches.add(new int[]{start, end});
                    }
                    temp = temp.failure;
                }
            }

            if (matches.isEmpty()) {
                return text;
            }

            // 按起始位置排序，从后往前替换（避免位置偏移）
            matches.sort((a, b) -> b[0] - a[0]);
            
            StringBuilder sb = new StringBuilder(text);
            for (int[] match : matches) {
                sb.replace(match[0], match[1], replacement);
            }

            return sb.toString();
            
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 判断是否包含敏感词
     */
    public boolean containsSensitiveWords(String text) {
        if (text == null || text.isEmpty() || root == null) {
            return false;
        }

        lock.readLock().lock();
        try {
            TrieNode current = root;
            String lowerText = text.toLowerCase();

            for (int i = 0; i < lowerText.length(); i++) {
                char c = lowerText.charAt(i);

                while (current != root && !current.children.containsKey(c)) {
                    current = current.failure;
                }

                if (current.children.containsKey(c)) {
                    current = current.children.get(c);
                }

                // 快速返回：一旦发现就返回 true
                TrieNode temp = current;
                while (temp != null && temp != root) {
                    if (temp.isEnd) {
                        return true;
                    }
                    temp = temp.failure;
                }
            }

            return false;
            
        } finally {
            lock.readLock().unlock();
        }
    }
}
```

### 5.2 使用示例

```java
public class AhoCorasickDemo {
    
    public static void main(String[] args) {
        AhoCorasickEngine engine = new AhoCorasickEngine();
        
        // 1. 初始化敏感词库
        Set<String> words = new HashSet<>();
        words.add("he");
        words.add("she");
        words.add("his");
        words.add("hers");
        engine.initialize(words);
        
        // 2. 检测敏感词
        String text = "ushers";
        List<String> hits = engine.findSensitiveWords(text);
        System.out.println("命中敏感词: " + hits);
        // 输出: 命中敏感词: [she, he, hers]
        
        // 3. 替换敏感词
        String replaced = engine.replaceSensitiveWords(text, "***");
        System.out.println("替换后: " + replaced);
        // 输出: 替换后: u***r***
        
        // 4. 判断是否包含
        boolean contains = engine.containsSensitiveWords(text);
        System.out.println("是否包含敏感词: " + contains);
        // 输出: 是否包含敏感词: true
    }
}
```

---

## 六、时间复杂度分析

| 阶段 | 时间复杂度 | 说明 |
|------|-----------|------|
| 构建 Trie 树 | O(∑len) | ∑len = 所有敏感词总长度 |
| 构建 Failure 指针 | O(∑len) | BFS 遍历所有节点 |
| 匹配 | **O(n)** | n = 待检测文本长度 |

**对比**：

| 方法 | 时间复杂度 | 5万敏感词 + 1万字文本 |
|------|-----------|----------------------|
| 暴力匹配 | O(n × m × k) | 约 50亿次操作 |
| AC 自动机 | O(n) | 约 1万次操作 |

---

## 七、项目中的实际应用

### 7.1 变形词绕过防护

用户可能用各种方式绕过检测：

```
原词：赌博
变形：赌 博（加空格）
     賭博（繁体）
     赌搏（同音字）
     d u b o（拼音）
     🎲博（emoji）
```

**解决方案**：在 AC 自动机检测前，先做文本预处理

```java
public class TextPreprocessor {
    
    /**
     * 文本预处理
     */
    public String preprocess(String text) {
        String result = text;
        
        // 1. 全角转半角
        result = fullToHalf(result);
        
        // 2. 繁体转简体
        result = traditionalToSimplified(result);
        
        // 3. 移除特殊字符和空格
        result = removeSpecialChars(result);
        
        // 4. 同音字还原
        result = replaceHomophone(result);
        
        // 5. 形近字还原
        result = replaceSimilarChar(result);
        
        return result.toLowerCase();
    }
    
    /**
     * 全角转半角
     * Ａ → A，１ → 1
     */
    private String fullToHalf(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            // 全角空格
            if (c == '\u3000') {
                sb.append(' ');
            }
            // 全角字符范围：0xFF01 ~ 0xFF5E
            else if (c >= 0xFF01 && c <= 0xFF5E) {
                sb.append((char) (c - 0xFEE0));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    /**
     * 移除特殊字符
     */
    private String removeSpecialChars(String text) {
        // 移除空格、零宽字符、标点等
        return text.replaceAll("[\\s\\p{Punct}\\u200B-\\u200D\\uFEFF]", "");
    }
}
```

### 7.2 完整检测流程

```java
public SensitiveCheckResponse checkText(String text) {
    // 1. 文本预处理（处理变形词）
    String preprocessed = textPreprocessor.preprocess(text);
    
    // 2. AC 自动机检测原文
    List<String> hits1 = acEngine.findSensitiveWords(text);
    
    // 3. AC 自动机检测预处理后的文本
    List<String> hits2 = acEngine.findSensitiveWords(preprocessed);
    
    // 4. 合并结果
    Set<String> allHits = new HashSet<>();
    allHits.addAll(hits1);
    allHits.addAll(hits2);
    
    // 5. 白名单过滤
    allHits.removeIf(word -> whitelist.contains(word));
    
    // 6. 返回结果
    return SensitiveCheckResponse.builder()
            .hit(!allHits.isEmpty())
            .hitWords(new ArrayList<>(allHits))
            .build();
}
```

---

## 八、面试常见问题

### Q1：AC 自动机和 Trie 树有什么区别？

> Trie 树只能一个一个模式串匹配，每次失败要回到根节点重新开始。
> AC 自动机在 Trie 树基础上加了 Failure 指针，失败时可以跳到其他位置继续匹配，实现一次扫描匹配所有模式串。

### Q2：Failure 指针是怎么构建的？

> 使用 BFS 层序遍历。对于每个节点，沿着其父节点的 failure 链向上找，看是否有相同字符的子节点。如果有，failure 就指向它；如果没有，继续向上找，直到根节点。

### Q3：为什么时间复杂度是 O(n)？

> 因为每个字符最多被访问常数次。虽然有 failure 指针的跳转，但每次跳转都会使"当前深度"减少，而深度的增加只发生在字符匹配成功时（最多 n 次），所以总的跳转次数是 O(n)。

### Q4：AC 自动机和 KMP 有什么关系？

> KMP 是单模式串匹配，AC 自动机是多模式串匹配。
> KMP 的 next 数组对应 AC 自动机的 failure 指针，思想是一样的：利用已匹配的信息，避免重复匹配。

### Q5：实际项目中如何保证并发安全？

> 使用读写锁（ReentrantReadWriteLock）。检测时用读锁（多个线程可以同时检测），更新词库时用写锁（独占）。

---

## 九、总结

AC 自动机是一种高效的多模式字符串匹配算法，核心思想是：

1. **Trie 树**：将所有模式串组织成树形结构
2. **Failure 指针**：匹配失败时，跳转到"最长后缀"继续匹配
3. **O(n) 复杂度**：只需扫描文本一遍，与模式串数量无关

在敏感词检测场景下，AC 自动机可以轻松支持数万级词库的毫秒级匹配，是内容风控系统的核心算法。
