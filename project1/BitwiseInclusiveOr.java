import java.lang.Integer;
import java.lang.System;

public class BitwiseInclusiveOr {
    public static void main(String[] args) {
        int a = Integer.parseInt(args[0]);
        int b = Integer.parseInt(args[1]);
        //int c = a | b;
        System.out.println(Integer.toBinaryString(a | b));
    }
}
