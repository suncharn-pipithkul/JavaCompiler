// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a try-catch-finally statement.
 */
class JTryStatement extends JStatement {
    // The try block.
    private JBlock tryBlock;

    // The catch parameters.
    private ArrayList<JFormalParameter> parameters;

    // The catch blocks.
    private ArrayList<JBlock> catchBlocks;

    // The finally block.
    private JBlock finallyBlock;

    /**
     * Constructs an AST node for a try-statement.
     *
     * @param line         line in which the while-statement occurs in the source file.
     * @param tryBlock     the try block.
     * @param parameters   the catch parameters.
     * @param catchBlocks  the catch blocks.
     * @param finallyBlock the finally block.
     */
    public JTryStatement(int line, JBlock tryBlock, ArrayList<JFormalParameter> parameters,
                         ArrayList<JBlock> catchBlocks, JBlock finallyBlock) {
        super(line);
        this.tryBlock = tryBlock;
        this.parameters = parameters;
        this.catchBlocks = catchBlocks;
        this.finallyBlock = finallyBlock;
    }

    /**
     * {@inheritDoc}
     */
    public JTryStatement analyze(Context context) {
        // Analyze try block
        LocalContext tryContext = new LocalContext(context);
        tryBlock = (JBlock) tryBlock.analyze(tryContext);

        // Analyze catch blocks
        for (int i = 0; i < parameters.size(); i++) {
            // analyze catch param
            // declare catch param in the new context
            LocalContext catchContext = new LocalContext(context);

            JFormalParameter param = parameters.get(i);
            int offset = catchContext.nextOffset(); // calculate local var offset
            Type paramType = param.type().resolve(catchContext); // resolve it in the new context
            param.setType(paramType); // set its type

            // declare the catch exception as local var
            LocalVariableDefn defn = new LocalVariableDefn(paramType, offset);

            // declare the var in the catch local context
            catchContext.addEntry(param.line(), param.name(), defn);
            defn.initialize();

            parameters.set(i, param); // set the analyzed param back into list

            // analyze catch block
            JBlock catchBlock = catchBlocks.get(i);
            catchBlock = (JBlock) catchBlock.analyze(catchContext);
            catchBlocks.set(i, catchBlock);
        }

        // Analyze finally block
        if (finallyBlock != null) {
            LocalContext finallyContext = new LocalContext(context);
            finallyBlock = (JBlock) finallyBlock.analyze(finallyContext);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        String startTryLabel = output.createLabel();
        String endTryLabel = output.createLabel();
        String startFinallyLabel = output.createLabel();
        String startFinallyPlusOneLabel = output.createLabel();
        String endFinallyLabel = output.createLabel();

        ArrayList<String> startCatchLabels = new ArrayList<>();
        ArrayList<String> endCatchLabels = new ArrayList<>();

        // generate try block
        output.addLabel(startTryLabel);
        tryBlock.codegen(output); // try body

        // generate optional finally block
        if (finallyBlock != null) {
            finallyBlock.codegen(output); // finally body
        }
        output.addBranchInstruction(GOTO, endFinallyLabel); // jump to end finally label
        output.addLabel(endTryLabel);

        // generate catch blocks
        for (int i = 0; i < catchBlocks.size(); i++) {
            String startCatchLabel = output.createLabel();
            String endCatchLabel = output.createLabel();

            startCatchLabels.add(startCatchLabel);
            endCatchLabels.add(endCatchLabel);

            output.addLabel(startCatchLabel); // start catch label
            output.addNoArgInstruction(ASTORE_1); // code to store catch var
            catchBlocks.get(i).codegen(output); // code for catch block body
            output.addLabel(endCatchLabel);// end catch label

            // exception handler
            String catchTypeStr = parameters.get(i).type().jvmName();
            output.addExceptionHandler(startTryLabel, endTryLabel, startCatchLabel, catchTypeStr);

            // code for optional finally block
            if (finallyBlock != null) {
                finallyBlock.codegen(output); // finally body
            }
            output.addBranchInstruction(GOTO, endFinallyLabel); // jump to end finally label
        }

        // generate finally block
        if (finallyBlock != null) {
            output.addLabel(startFinallyLabel); // start finally label

            // generate ASTORE with offset obtained from the context for the finally block
            int finallyOffset = parameters.size() + 2;
            output.addOneArgInstruction(ASTORE, finallyOffset);

            // start finally plus one label
            output.addLabel(startFinallyPlusOneLabel);

            // code for finally block
            finallyBlock.codegen(output);

            // generate ALOAD with the offset
            output.addOneArgInstruction(ALOAD, finallyOffset);

            // generate ATHROW instruction
            output.addNoArgInstruction(ATHROW);

            // end finally label
            output.addLabel(endFinallyLabel);

            // exception handler(start catch, end catch, start finally, null)
            for (int i = 0; i < startCatchLabels.size(); i++) {
                output.addExceptionHandler(startCatchLabels.get(i), endCatchLabels.get(i),
                        startFinallyLabel, null);
            }

            // exception handler(start finally, start finally plus one, start finally, null)
            output.addExceptionHandler(startFinallyLabel, startFinallyPlusOneLabel,
                    startFinallyLabel, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JTryStatement:" + line, e);
        JSONElement e1 = new JSONElement();
        e.addChild("TryBlock", e1);
        tryBlock.toJSON(e1);
        if (catchBlocks != null) {
            for (int i = 0; i < catchBlocks.size(); i++) {
                JFormalParameter param = parameters.get(i);
                JBlock catchBlock = catchBlocks.get(i);
                JSONElement e2 = new JSONElement();
                e.addChild("CatchBlock", e2);
                String s = String.format("[\"%s\", \"%s\"]", param.name(), param.type() == null ?
                        "" : param.type().toString());
                e2.addAttribute("parameter", s);
                catchBlock.toJSON(e2);
            }
        }
        if (finallyBlock != null) {
            JSONElement e2 = new JSONElement();
            e.addChild("FinallyBlock", e2);
            finallyBlock.toJSON(e2);
        }
    }
}
