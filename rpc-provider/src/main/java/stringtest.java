import com.google.common.annotations.VisibleForTesting;

public class stringtest {
    public static void main(String[] args) {
        String s1 = "a";
        String s2 = "b";
        String s3 = s1 + s2;
        String s4 = s1 + s2;
        System.out.println(s3 == s4);
        System.out.println(s3 == s4.intern());
        System.out.println(s3.intern() == s4.intern());
        System.out.println(s3 == s3.intern());
        System.out.println(s1 == s1.intern());
    }
}
