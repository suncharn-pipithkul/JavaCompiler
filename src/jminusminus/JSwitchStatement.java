// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import javax.swing.text.Style;
import java.util.ArrayList;
import java.util.TreeMap;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a switch-statement.
 */
public class JSwitchStatement extends JStatement {
    // Test expression.
    private JExpression condition;

    // List of switch-statement groups.
    private ArrayList<SwitchStatementGroup> stmtGroup;

    // switch labels var to help calculate opcode
    private long hi;
    private long lo;
    private ArrayList<String> labels;
    private TreeMap<Integer, String> matchLabelPairs;
    private int nLabels; // count number of labels

    // break;
    public boolean hasBreak;
    public String breakLabel;

    /**
     * Constructs an AST node for a switch-statement.
     *
     * @param line      line in which the switch-statement occurs in the source file.
     * @param condition test expression.
     * @param stmtGroup list of statement groups.
     */
    public JSwitchStatement(int line, JExpression condition,
                            ArrayList<SwitchStatementGroup> stmtGroup) {
        super(line);
        this.condition = condition;
        this.stmtGroup = stmtGroup;
        this.hasBreak = false;
    }

    /**
     * {@inheritDoc}
     */
    public JStatement analyze(Context context) {
        JMember.enclosingStatement.push(this);

        condition = (JExpression) condition.analyze(context);
        condition.type().mustMatchExpected(line(), Type.INT);

        for (SwitchStatementGroup group : stmtGroup) {
            LocalContext localContext = new LocalContext(context);

            // Analyze each case to make sure it's literal int
            for (int i = 0; i < group.switchLabels.size(); i++) {
                JExpression label = group.switchLabels.get(i);

                // If there is a case label there (not default)
                if (label != null) {
                    JLiteralInt analyzedLabel = (JLiteralInt) label.analyze(localContext);
                    group.switchLabels.set(i, analyzedLabel);
                    analyzedLabel.type().mustMatchExpected(line(), Type.INT);
                }
            }

            // Analyze each block
            for (int i = 0; i < group.block.size(); i++) {
                group.block.set(i, (JStatement) group.block.get(i).analyze(localContext));
            }
        }

        JMember.enclosingStatement.pop();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        condition.codegen(output);

        // Initialize values
        boolean hasDefault = false;
        hi = Long.MIN_VALUE;
        lo = Long.MAX_VALUE;
        labels = new ArrayList<String>();
        nLabels = 0;
        matchLabelPairs = new TreeMap<Integer, String>();

        // Initialize general use label
        String defaultLabel = output.createLabel();
        String exitLabel = output.createLabel();
        breakLabel = output.createLabel();

        // calculate values to figure the correct opCode
        for (SwitchStatementGroup group : stmtGroup) {
            for (JExpression label : group.switchLabels) {
                if (label != null) { // If there is a case label there (not default)
                    int value = ((JLiteralInt) label).toInt();
                    if (value > hi) {
                        hi = value;
                    }

                    if (value < lo) {
                        lo = value;
                    }
                    nLabels++;

                    String labelStr = output.createLabel();
                    labels.add(labelStr);
                    matchLabelPairs.put(value, labelStr);
                } else {
                    hasDefault = true;
                }
            }
        }

        // algorithm to calculate opcode from project instruction
        long tableSpaceCost = 5 + hi - lo;
        long tableTimeCost = 3;
        long lookupSpaceCost = 3 + 2 * (long) nLabels;
        long lookupTimeCost = nLabels;
        int opcode = nLabels > 0 && (tableSpaceCost + 3 * tableTimeCost <= lookupSpaceCost + 3 * lookupTimeCost) ?
                TABLESWITCH : LOOKUPSWITCH;


        // generate the switch table
        if (opcode == TABLESWITCH) { // sequential label case
            output.addTABLESWITCHInstruction(hasDefault? defaultLabel : exitLabel, (int)lo, 4, labels);
        } else { // sparse label case
            output.addLOOKUPSWITCHInstruction(hasDefault? defaultLabel : exitLabel, nLabels, matchLabelPairs);
        }

        // generate the code for each case/default
        int caseIndex = 0;
        for (SwitchStatementGroup group : stmtGroup) {
            // Generate the case labels for this statement group
            for (JExpression label : group.switchLabels) {
                if (label != null) {
                    int value = ((JLiteralInt) label).toInt();
                    String labelStr = opcode == TABLESWITCH ?
                            labels.get(caseIndex++) : matchLabelPairs.get(value);
                    output.addLabel(labelStr);
                } else {
                    output.addLabel(defaultLabel);
                }
            }

            // Generate the code body for this statement group
            for (JStatement statement : group.block) {
                statement.codegen(output);
            }
        }

        if (hasBreak) {
            output.addLabel(breakLabel);
        }
        output.addLabel(exitLabel);
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JSwitchStatement:" + line, e);
        JSONElement e1 = new JSONElement();
        e.addChild("Condition", e1);
        condition.toJSON(e1);
        for (SwitchStatementGroup group : stmtGroup) {
            group.toJSON(e);
        }
    }
}

/**
 * A switch statement group consists of case labels and a block of statements.
 */
class SwitchStatementGroup {
    // Case labels.
    public ArrayList<JExpression> switchLabels;

    // Block of statements.
    public ArrayList<JStatement> block;

    /**
     * Constructs a switch-statement group.
     *
     * @param switchLabels case labels.
     * @param block        block of statements.
     */
    public SwitchStatementGroup(ArrayList<JExpression> switchLabels, ArrayList<JStatement> block) {
        this.switchLabels = switchLabels;
        this.block = block;
    }

    /**
     * Stores information about this switch statement group in JSON format.
     *
     * @param json the JSON emitter.
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("SwitchStatementGroup", e);
        for (JExpression label : switchLabels) {
            JSONElement e1 = new JSONElement();
            if (label != null) {
                e.addChild("Case", e1);
                label.toJSON(e1);
            } else {
                e.addChild("Default", e1);
            }
        }
        if (block != null) {
            for (JStatement stmt : block) {
                stmt.toJSON(e);
            }
        }
    }
}
