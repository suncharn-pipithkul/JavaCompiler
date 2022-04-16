import java.util.ArrayList;

import jminusminus.CLEmitter;

import static jminusminus.CLConstants.*;

/**
 * This class programmatically generates the class file for the following Java application:
 * 
 * <pre>
 * public class IsPrime {
 *     // Entry point.
 *     public static void main(String[] args) {
 *         int n = Integer.parseInt(args[0]);
 *         boolean result = isPrime(n);
 *         if (result) {
 *             System.out.println(n + " is a prime number");
 *         } else {
 *             System.out.println(n + " is not a prime number");
 *         }
 *     }
 *
 *     // Returns true if n is prime, and false otherwise.
 *     private static boolean isPrime(int n) {
 *         if (n < 2) {
 *             return false;
 *         }
 *         for (int i = 2; i <= n / i; i++) {
 *             if (n % i == 0) {
 *                 return false;
 *             }
 *         }
 *         return true;
 *     }
 * }
 * </pre>
 */
public class GenIsPrime {
    public static void main(String[] args) {
        // Create a CLEmitter instance
        CLEmitter e = new CLEmitter(true);

        // Create an ArrayList instance to store modifiers
        ArrayList<String> modifiers = new ArrayList<String>();

        // public class IsPrime {
        modifiers.add("public");
        e.addClass(modifiers, "IsPrime","java/lang/Object", null, true);

        // public static void main(String[] args) {
        modifiers.clear();
        modifiers.add("public");
        modifiers.add("static");
        e.addMethod(modifiers, "main", "([Ljava/lang/String;)V", null, true);

        // int n = Integer.parseInt(args[0]);
        e.addNoArgInstruction(ALOAD_0); // push args into the stack
        e.addNoArgInstruction(ICONST_0); // push constant 0 into the stack
        e.addNoArgInstruction(AALOAD); // pop args & 0 and push args[0] in
        e.addMemberAccessInstruction(INVOKESTATIC, "java/lang/Integer", "parseInt",
                "(Ljava/lang/String;)I");
        e.addNoArgInstruction(ISTORE_1); // assign n = ...

        // boolean result = isPrime(n);
        e.addNoArgInstruction(ILOAD_1); // push n into stack
        e.addMemberAccessInstruction(INVOKESTATIC, "IsPrime", "isPrime", "(I)I");
        e.addNoArgInstruction(ISTORE_2); // result = ...

        // if (!result) branch to "Not Prime"
        e.addNoArgInstruction(ILOAD_2); // push result into stack
        e.addBranchInstruction(IFEQ, "Not Prime"); // if result == 0

        // System.out.println(n + " is a prime number");
        // Get System.out on stack
        e.addMemberAccessInstruction(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");

        // Create an instance (say sb) of StringBuffer on stack for string concatenations
        //    sb = new StringBuffer();
        e.addReferenceInstruction(NEW, "java/lang/StringBuffer");
        e.addNoArgInstruction(DUP);
        e.addMemberAccessInstruction(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "()V");

        // sb.append(n);
        e.addNoArgInstruction(ILOAD_1);
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                "(I)Ljava/lang/StringBuffer;");

        // sb.append(" is a prime number");
        e.addLDCInstruction(" is a prime number");
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuffer;");

        // System.out.println(sb.toString());
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/lang/StringBuffer",
                "toString", "()Ljava/lang/String;");
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V");

        e.addBranchInstruction(GOTO, "end");

        // else {
        e.addLabel("Not Prime");

        // System.out.println(n + " is not a prime number");
        // Get System.out on stack
        e.addMemberAccessInstruction(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");

        // Create an instance (say sb) of StringBuffer on stack for string concatenations
        //    sb = new StringBuffer();
        e.addReferenceInstruction(NEW, "java/lang/StringBuffer");
        e.addNoArgInstruction(DUP);
        e.addMemberAccessInstruction(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "()V");

        // sb.append(n);
        e.addNoArgInstruction(ILOAD_1);
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                "(I)Ljava/lang/StringBuffer;");

        // sb.append(" is not a prime number");
        e.addLDCInstruction(" is not a prime number");
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuffer;");

        // System.out.println(sb.toString());
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/lang/StringBuffer",
                "toString", "()Ljava/lang/String;");
        e.addMemberAccessInstruction(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V");

        // return;
        e.addLabel("end");
        e.addNoArgInstruction(RETURN);

        // private static boolean isPrime(int n) {
        modifiers.clear();
        modifiers.add("private");
        modifiers.add("static");
        e.addMethod(modifiers, "isPrime", "(I)I", null, true);

        //     if (n >= 2) branch to "for init"
        e.addNoArgInstruction(ILOAD_0); // push n
        e.addNoArgInstruction(ICONST_2); // push 2
        e.addBranchInstruction(IF_ICMPGE, "for init"); // n >= 2

        //          return false;
        e.addNoArgInstruction(ICONST_0); // push 0 (false)
        e.addNoArgInstruction(IRETURN);
        //     }

        //     for (int i = 2; i <= n / i; i++) {
        //      for init: i = 2
        e.addLabel("for init");
        e.addNoArgInstruction(ICONST_2); // push 2
        e.addNoArgInstruction(ISTORE_1); // i = 2

        //      for condition: if i > n / i goto "for out":
        e.addLabel("for condition");
        e.addNoArgInstruction(ILOAD_1); // push i
        e.addNoArgInstruction(ILOAD_0); // push n
        e.addNoArgInstruction(ILOAD_1); // push i
        e.addNoArgInstruction(IDIV); // n / i
        e.addBranchInstruction(IF_ICMPGT, "for out"); // i > n / i then exit for loop

        e.addNoArgInstruction(ILOAD_0); // push n
        e.addNoArgInstruction(ILOAD_1); // push i
        e.addNoArgInstruction(IREM); // n % i
        e.addNoArgInstruction(ICONST_0); // push 0
        e.addBranchInstruction(IF_ICMPNE, "for update"); // i > n / i
        e.addNoArgInstruction(ICONST_0); // push 0 (false)
        e.addNoArgInstruction(IRETURN);

        //      for update: increment i by 1; branch to "for condition"
        e.addLabel("for update");
        e.addIINCInstruction(1, 1); // i++
        e.addBranchInstruction(GOTO, "for condition");

        //      for out:   return  True
        e.addLabel("for out");
        e.addNoArgInstruction(ICONST_1); // push 1 (true)
        e.addNoArgInstruction(IRETURN);

        // Write IsPrime.class to file system
        e.write();
    }
}
