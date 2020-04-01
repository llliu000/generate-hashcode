package com.gitee;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Random;

/**
 * “中间相遇法”是生日攻击的一种变形，它不比较Hash值，而是比较链中的中间变量。这种攻击主要适用于攻击具有分组链结构的Hash方案。中间相遇攻击的基本原理为：将消息分成两部分，对伪造消息的第一部分从初试值开始逐步向中间阶段产生r1个变量；对伪造消息的第二部分从Hash结果开始逐步退回中间阶段产生r2个变量。在中间阶段有一个匹配的概率与生日攻击成功的概率一样。
 */
public class HashCollide {

    /**
     * 拼凑字符的起始值(最后实际值可能为 collideCharBase +- mulBase)
     */
    private int collideCharBase;

    /**
     * 中间变量
     */
    private BigDecimal collideCharBase_decimal;

    /**
     * 中间变量
     */
    private BigDecimal mulBase_decimal_pow;

    /**
     * 拼凑字符串长度
     */
    private int collideCharLength;

    /**
     * hash算法中采用的乘积值 (hash' = hash * mulBase + char[i])
     */
    private long mulBase;

    /**
     * 中间变量
     */
    private BigDecimal mulBase_decimal;

    /**
     * 中间变量
     */
    private long mulBase_desc;

    /**
     * 中间变量
     */
    private BigDecimal mulBase_desc_decimal;

    /**
     * 2的轮回...
     */
    private final long INT_ROUTE_NUMBER = 2l << 32;

    /**
     * 还是2的轮回...
     */
    private final BigDecimal DECIMAL_ROUTE_NUMBER = new BigDecimal(
            INT_ROUTE_NUMBER);

    /**
     * 不知道干啥的,好奇怪
     */
    private final Random random = new Random(System.nanoTime());

    /**
     * 测试你的char数组能吧srcHash变成什么样子
     *
     * @param srcHash
     * @param collide
     * @return
     */
    public int hashCodeTest(int srcHash, char collide[]) {
        int h = srcHash;
        int len = collide.length;
        for (int i = 0; i < len; i++) {
            h = (int) mulBase * h + collide[i];
        }
        return h;
    }

    /**
     * 根据这个类构造时设置的参数输出hash
     *
     * @param srcString
     * @return
     */
    public int hashCodeTest(String srcString) {
        char[] chars = srcString.toCharArray();

        int h = 0;
        int len = chars.length;
        for (int i = 0; i < len; i++) {
            h = (int) mulBase * h + chars[i];
        }
        return h;
    }

    /**
     * 将一个decimal的值通过取余转换成一个属于int范围的long
     *
     * @param data
     * @return
     */
    private long fixDecimal(BigDecimal data) {
        // 求余数    
        BigDecimal sub = data.divideToIntegralValue(DECIMAL_ROUTE_NUMBER
                .multiply(DECIMAL_ROUTE_NUMBER));

        // 可能为负数,修正为long类型之后再次求余    
        long val = data.subtract(sub).longValue();
        val += INT_ROUTE_NUMBER;
        val = val % INT_ROUTE_NUMBER;

        if (val < 0) // val应该不会小于0    
            val += INT_ROUTE_NUMBER;
        return val;
    }

    /**
     * 把val转换为正序的char数组,用以表示一个n位k进制数据
     *
     * @param val
     * @return
     */
    private char[] offsetToArray(long val) {
        char[] stk = new char[collideCharLength];
        int pos = 0;

        while (val != 0) { // 进制转换,得到反序列    
            stk[pos++] = (char) (val % (mulBase) + collideCharBase);
            val = val / mulBase;
        }

        int fillZero = collideCharLength - pos; // 补零的个数    
        char[] collides = new char[collideCharLength];
        int i = 0;
        while (i < fillZero) { // 高位补零    
            collides[i++] = (char) collideCharBase;
        }

        while (i < collideCharLength) { // 逐位反向输出    
            collides[i] = stk[pos - i + fillZero - 1]; // pos - ( i - fillZero )    
            ++i;
        }

        return collides;
    }

    /**
     * 根据hash的src和target生成一组序列，使原串后面附加序列字符后的hash与target相同
     *
     * @param src
     * @param target
     * @return
     */
    private char[] genCollisionArray(int src, int target) {
        long hx = mulBase_desc * src + collideCharBase;
        BigDecimal halfCal = mulBase_decimal_pow.multiply(new BigDecimal(hx)) // 中间变量    
                .subtract(collideCharBase_decimal);
        BigDecimal left = halfCal.divide(mulBase_desc_decimal); // 依然是中间变量    
        BigDecimal fix = new BigDecimal(target).subtract(left); // 还是中间变量,不过这次是修正数据    

        long fixedDecimal = fixDecimal(fix);

        return offsetToArray(fixedDecimal);
    }

    /**
     * 构造函数
     *
     * @param collideCharBase   拼凑字符的起始值(最后实际值可能为 collideCharBase +- mulBase)
     * @param collideCharLength 拼凑字符串长度
     * @param mulBase           hash算法中采用的乘积值 (hash' = hash * mulBase + char[i])
     */
    public HashCollide(int collideCharBase, int collideCharLength, int mulBase) {
        this.mulBase = mulBase;
        this.mulBase_decimal = new BigDecimal(mulBase);
        this.mulBase_desc = mulBase - 1;
        this.mulBase_desc_decimal = new BigDecimal(mulBase - 1);

        this.mulBase_decimal_pow = mulBase_decimal.pow(collideCharLength);

        this.collideCharBase = collideCharBase;
        this.collideCharBase_decimal = new BigDecimal(collideCharBase);
        this.collideCharLength = collideCharLength;

    }

    /**
     * ...
     *
     * @param source
     * @param targetHash
     * @return
     */
    public String collide(String source, int targetHash) {
        int hashSrc = source.hashCode();
        char[] collide = this.genCollisionArray(hashSrc, targetHash);
        return source.concat(new String(collide));
    }

    /**
     * ...
     *
     * @return
     */
    public String randomString(int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; ++i) {
            chars[i] = (char) (50 + random.nextInt(32 + 26 + 15));
        }
        return new String(chars);
    }

    public static void main(String[] args) {

        System.out.println("NH]n;<pFEP2R4DYDw4Fsg^Z[gTHIx=e4H:hSEVtfR^SqsP]y[gK_:gIgvZccbUmj".hashCode());
        System.out.println("W@3Lb=SIr`\\h?KT5?ayLtd37y]x=\\Pcibr4O4SaKubhJ_M`lqEbjk;5ycYmZp^cm".hashCode());

        System.out.println("====================");

        HashCollide collide = new HashCollide(85, 7, 31);

        HashMap<String, Object> hashMap = new HashMap<>();
        for (int i = 0; i < 10; ++i) {

            String a = collide.randomString(57);
            String b = collide.collide(a, 912366222);
            System.out.println(b.hashCode());
            hashMap.put(b, i);

            if (b.hashCode() != 912366222) {
                System.err.println("ERROR :: src = " + a);
                System.err.println("ERROR :: dst = " + b);
                System.exit(1);
            }
        }

    }
}
