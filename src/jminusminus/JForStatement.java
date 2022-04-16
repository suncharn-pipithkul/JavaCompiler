// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a for-statement.
 */
class JForStatement extends JStatement {
    // Initialization.
    private ArrayList<JStatement> init;

    // Test expression
    private JExpression condition;

    // Update.
    private ArrayList<JStatement> update;

    // The body.
    private JStatement body;

    // break;
    public boolean hasBreak;
    public String breakLabel;

    // continue;
    public boolean hasContinue;
    public String continueLabel;

    /**
     * Constructs an AST node for a for-statement.
     *
     * @param line      line in which the for-statement occurs in the source file.
     * @param init      the initialization.
     * @param condition the test expression.
     * @param update    the update.
     * @param body      the body.
     */
    public JForStatement(int line, ArrayList<JStatement> init, JExpression condition,
                         ArrayList<JStatement> update, JStatement body) {
        super(line);
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
        this.hasBreak = false;
    }

    /**
     * {@inheritDoc}
     */
    public JForStatement analyze(Context context) {
        JMember.enclosingStatement.push(this);
        LocalContext localContext = new LocalContext(context);

        // Analyze for init
        if (init != null) {
            ArrayList<JStatement> tempInit = new ArrayList<>();
            for (JStatement i : init) {
                i = (JStatement) i.analyze(localContext);
                tempInit.add(i);
            }
            init = tempInit;
        }

        // Analyze condition
        if (condition != null) {
            condition = (JExpression) condition.analyze(localContext);
        }

        // Analyze the update
        if (update != null) {
            ArrayList<JStatement> tempUpdate = new ArrayList<>();
            for (JStatement u : update) {
                u = (JStatement) u.analyze(localContext);
                tempUpdate.add(u);
            }
            update = tempUpdate;
        }

        // Analyze the body
        if (body != null) {
            body = (JStatement) body.analyze(localContext);
        }

        JMember.enclosingStatement.pop();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        // create labels
        String startForLabel = output.createLabel();
        String exitLabel = output.createLabel();
        breakLabel = output.createLabel();
        continueLabel = output.createLabel();

        // generate initialize for loop vars
        if (init != null) {
            for (JStatement i : init) {
                i.codegen(output);
            }
        }

        // generate condition
        output.addLabel(startForLabel);
        if (condition != null) {
            condition.codegen(output, exitLabel, false); // exit the loop on false condition
        }

        // body of for loop
        if (body != null) {
            body.codegen(output);
        }

        if (hasContinue) {  // if body has continue, jump here out of body
            output.addLabel(continueLabel);
        }

        // generate update
        if (update != null) {
            for (JStatement u : update) {
                u.codegen(output);
            }
        }

        // jump back to checking condition
        output.addBranchInstruction(GOTO, startForLabel);

        output.addLabel(exitLabel); // exit for loop label
        if (hasBreak) {
            output.addLabel(breakLabel);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JForStatement:" + line, e);
        if (init != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Init", e1);
            for (JStatement stmt : init) {
                stmt.toJSON(e1);
            }
        }
        if (condition != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Condition", e1);
            condition.toJSON(e1);
        }
        if (update != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Update", e1);
            for (JStatement stmt : update) {
                stmt.toJSON(e1);
            }
        }
        if (body != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Body", e1);
            body.toJSON(e1);
        }
    }
}
