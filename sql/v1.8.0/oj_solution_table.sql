-- =============================================
-- OJ 标准答案 (题解) 建表 + 初始数据
-- =============================================

CREATE TABLE IF NOT EXISTS `oj_solution` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题解ID',
    `problem_id` BIGINT NOT NULL COMMENT '题目ID',
    `language` VARCHAR(20) NOT NULL COMMENT '编程语言',
    `title` VARCHAR(255) COMMENT '题解标题',
    `code` TEXT NOT NULL COMMENT '标准答案代码',
    `description` TEXT COMMENT '题解说明 (Markdown)',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`id`),
    INDEX `idx_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OJ标准答案表';


-- =============================================
-- 10 道题的 Java 标准答案
-- =============================================

-- 1. 两数之和
INSERT INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(1, 'java', '哈希表解法',
'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();
        int target = sc.nextInt();

        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement)) {
                int a = map.get(complement), b = i;
                System.out.println(Math.min(a, b) + " " + Math.max(a, b));
                return;
            }
            map.put(nums[i], i);
        }
    }
}',
'## 思路\n\n使用哈希表存储已遍历的元素及其下标。对于当前元素 `nums[i]`，查找 `target - nums[i]` 是否已在哈希表中。\n\n## 复杂度\n- 时间: O(n)\n- 空间: O(n)',
1);

-- 2. 反转字符串
INSERT INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(2, 'java', '双指针 / StringBuilder',
'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        System.out.println(new StringBuilder(s).reverse().toString());
    }
}',
'## 思路\n\n利用 `StringBuilder.reverse()` 一行搞定。也可以用双指针交换首尾字符。\n\n## 复杂度\n- 时间: O(n)\n- 空间: O(n)',
1);

-- 3. 有效的括号
INSERT INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(3, 'java', '栈匹配',
'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            String s = sc.nextLine().trim();
            if (s.isEmpty()) continue;
            System.out.println(isValid(s));
        }
    }

    static boolean isValid(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            if (c == ''('') stack.push('')'');
            else if (c == ''['') stack.push('']'');
            else if (c == ''{'') stack.push(''}'');
            else if (stack.isEmpty() || stack.pop() != c) return false;
        }
        return stack.isEmpty();
    }
}',
'## 思路\n\n遇到左括号时，将对应的右括号压栈。遇到右括号时，弹出栈顶比较是否匹配。最后栈为空则有效。\n\n## 复杂度\n- 时间: O(n)\n- 空间: O(n)',
1);

-- 4. 最大子数组和
INSERT INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(4, 'java', 'Kadane 算法',
'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int maxSum = Integer.MIN_VALUE, curSum = 0;
        for (int i = 0; i < n; i++) {
            int num = sc.nextInt();
            curSum = Math.max(num, curSum + num);
            maxSum = Math.max(maxSum, curSum);
        }
        System.out.println(maxSum);
    }
}',
'## 思路\n\nKadane 算法：维护当前子数组和 `curSum`，如果 `curSum` 加上当前元素还不如当前元素本身大，就从当前元素重新开始。每一步更新全局最大值。\n\n## 复杂度\n- 时间: O(n)\n- 空间: O(1)',
1);

-- 5. 二分查找
INSERT INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(5, 'java', '标准二分查找',
'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();
        int target = sc.nextInt();

        int lo = 0, hi = n - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (nums[mid] == target) {
                System.out.println(mid);
                return;
            } else if (nums[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        System.out.println(-1);
    }
}',
'## 思路\n\n经典二分查找：维护 `[lo, hi]` 区间，每次取中间值与 `target` 比较，缩小一半搜索范围。\n\n## 复杂度\n- 时间: O(log n)\n- 空间: O(1)',
1);

-- 6. 斐波那契数
INSERT INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(6, 'java', '迭代法',
'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        if (n <= 1) {
            System.out.println(n);
            return;
        }
        int a = 0, b = 1;
        for (int i = 2; i <= n; i++) {
            int temp = a + b;
            a = b;
            b = temp;
        }
        System.out.println(b);
    }
}',
'## 思路\n\n用两个变量滚动计算，避免递归的重复计算。从 `F(0)=0, F(1)=1` 开始，每步计算 `F(i) = F(i-1) + F(i-2)`。\n\n## 复杂度\n- 时间: O(n)\n- 空间: O(1)',
1);

-- 7. 合并两个有序数组
INSERT INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(7, 'java', '双指针合并',
'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int m = Integer.parseInt(sc.nextLine().trim());
        int[] a = m > 0 ? Arrays.stream(sc.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray() : new int[0];
        if (m == 0) sc.nextLine(); // skip empty line
        int n = Integer.parseInt(sc.nextLine().trim());
        int[] b = n > 0 ? Arrays.stream(sc.nextLine().trim().split(" ")).mapToInt(Integer::parseInt).toArray() : new int[0];

        int[] result = new int[m + n];
        int i = 0, j = 0, k = 0;
        while (i < m && j < n) {
            result[k++] = a[i] <= b[j] ? a[i++] : b[j++];
        }
        while (i < m) result[k++] = a[i++];
        while (j < n) result[k++] = b[j++];

        StringBuilder sb = new StringBuilder();
        for (int idx = 0; idx < result.length; idx++) {
            if (idx > 0) sb.append(" ");
            sb.append(result[idx]);
        }
        System.out.println(sb);
    }
}',
'## 思路\n\n使用双指针分别指向两个数组头部，每次取较小的放入结果数组，直到其中一个遍历完，再把另一个的剩余部分追加。\n\n## 复杂度\n- 时间: O(m+n)\n- 空间: O(m+n)',
1);

-- 8. 爬楼梯
INSERT INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(8, 'java', '动态规划',
'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        if (n <= 2) {
            System.out.println(n);
            return;
        }
        int a = 1, b = 2;
        for (int i = 3; i <= n; i++) {
            int temp = a + b;
            a = b;
            b = temp;
        }
        System.out.println(b);
    }
}',
'## 思路\n\n到达第 `n` 阶的方式 = 到达第 `n-1` 阶的方式 + 到达第 `n-2` 阶的方式，本质就是斐波那契数列。用滚动变量 O(1) 空间完成。\n\n## 复杂度\n- 时间: O(n)\n- 空间: O(1)',
1);

-- 9. 最长递增子序列
INSERT INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(9, 'java', '动态规划 O(n²)',
'import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();

        int[] dp = new int[n];
        int ans = 1;
        for (int i = 0; i < n; i++) {
            dp[i] = 1;
            for (int j = 0; j < i; j++) {
                if (nums[j] < nums[i]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            ans = Math.max(ans, dp[i]);
        }
        System.out.println(ans);
    }
}',
'## 思路\n\n`dp[i]` 表示以 `nums[i]` 结尾的最长递增子序列长度。对每个 `i`，遍历 `j < i`，若 `nums[j] < nums[i]`，则 `dp[i] = max(dp[i], dp[j]+1)`。\n\n## 复杂度\n- 时间: O(n²)\n- 空间: O(n)\n\n> 进阶：可用贪心+二分优化到 O(n log n)',
1);

-- 10. 零钱兑换
INSERT INTO `oj_solution` (`problem_id`, `language`, `title`, `code`, `description`, `sort_order`) VALUES
(10, 'java', '完全背包 DP',
'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] coins = new int[n];
        for (int i = 0; i < n; i++) coins[i] = sc.nextInt();
        int amount = sc.nextInt();

        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1);
        dp[0] = 0;
        for (int coin : coins) {
            for (int j = coin; j <= amount; j++) {
                dp[j] = Math.min(dp[j], dp[j - coin] + 1);
            }
        }
        System.out.println(dp[amount] > amount ? -1 : dp[amount]);
    }
}',
'## 思路\n\n完全背包问题。`dp[j]` 表示凑成金额 `j` 所需的最少硬币数。对每种硬币，从 `coin` 到 `amount` 遍历更新：`dp[j] = min(dp[j], dp[j-coin]+1)`。\n\n## 复杂度\n- 时间: O(n × amount)\n- 空间: O(amount)',
1);
