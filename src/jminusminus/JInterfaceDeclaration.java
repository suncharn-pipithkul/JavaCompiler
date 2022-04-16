// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;

/**
 * A representation of an interface declaration.
 */
class JInterfaceDeclaration extends JAST implements JTypeDecl {
    // Interface modifiers.
    private ArrayList<String> mods;

    // Interface name.
    private String name;

    // This interface type.
    private Type thisType;

    // Super class type.
    private Type superType;

    // Extended interfaces.
    private ArrayList<TypeName> superInterfaces;

    // Interface block.
    private ArrayList<JMember> interfaceBlock;

    // Context for this interface.
    private ClassContext context;

    /**
     * Constructs an AST node for an interface declaration.
     *
     * @param line            line in which the interface declaration occurs in the source file.
     * @param mods            class modifiers.
     * @param name            class name.
     * @param superInterfaces super class types.
     * @param interfaceBlock  interface block.
     */
    public JInterfaceDeclaration(int line, ArrayList<String> mods, String name,
                                 ArrayList<TypeName> superInterfaces,
                                 ArrayList<JMember> interfaceBlock) {
        super(line);
        this.mods = mods;
        this.name = name;
        this.superType = Type.OBJECT;
        this.superInterfaces = superInterfaces;
        this.interfaceBlock = interfaceBlock;

        // implicit modifiers
        if (this.mods == null)
            this.mods = new ArrayList<>();
        this.mods.add("abstract");
        this.mods.add("interface");
    }

    /**
     * {@inheritDoc}
     */
    public void declareThisType(Context context) {
        String qualifiedName = JAST.compilationUnit.packageName() == "" ?
                name : JAST.compilationUnit.packageName() + "/" + name;
        CLEmitter partial = new CLEmitter(false);
        partial.addClass(mods, qualifiedName, Type.OBJECT.jvmName(), null, false);
        thisType = Type.typeFor(partial.toClass());
        context.addType(line, thisType);
    }

    /**
     * {@inheritDoc}
     */
    public void preAnalyze(Context context) {
        // Construct a class context.
        this.context = new ClassContext(this, context);

        // Create the (partial) class.
        CLEmitter partial = new CLEmitter(false);

        // Add the class header to the partial class
        String qualifiedName = JAST.compilationUnit.packageName() == "" ?
                name : JAST.compilationUnit.packageName() + "/" + name;
        partial.addClass(mods, qualifiedName, superType.jvmName(), null, false);

        // Pre-analyze the members and add them to the partial class.
        for (JMember member : interfaceBlock) {
            member.preAnalyze(this.context, partial);
        }

        // Get the ClassRep for the (partial) class and make it the representation for this type.
        Type id = this.context.lookupType(name);
        if (id != null && !JAST.compilationUnit.errorHasOccurred()) {
            id.setClassRep(partial.toClass());
        }
    }

    /**
     * {@inheritDoc}
     */
    public String name() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Type superType() {
        return superType;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayList<TypeName> superInterfaces() {
        return superInterfaces;
    }

    /**
     * {@inheritDoc}
     */
    public Type thisType() {
        return thisType;
    }

    /**
     * {@inheritDoc}
     */
    public JAST analyze(Context context) {
        // Analyze all members.
        for (JMember member : interfaceBlock) {
            ((JAST) member).analyze(this.context);
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        // The class header.
        String qualifiedName = JAST.compilationUnit.packageName() == "" ?
                name : JAST.compilationUnit.packageName() + "/" + name;

        // Get every super interfaces name as strings
        ArrayList<String> interfaces = new ArrayList<>();
        if (superInterfaces != null) {
            for (TypeName interfaceTypeName : superInterfaces) {
                interfaces.add(interfaceTypeName.jvmName());
            }
        }
        output.addClass(mods, qualifiedName, superType.jvmName(), interfaces, false);

        // The members.
        for (JMember member : interfaceBlock) {
            ((JAST) member).codegen(output);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JInterfaceDeclaration:" + line, e);
        if (mods != null) {
            ArrayList<String> value = new ArrayList<String>();
            for (String mod : mods) {
                value.add(String.format("\"%s\"", mod));
            }
            e.addAttribute("modifiers", value);
        }
        e.addAttribute("name", name);
        e.addAttribute("super", superType == null ? "" : superType.toString());
        if (superInterfaces != null) {
            ArrayList<String> value = new ArrayList<String>();
            for (TypeName impl : superInterfaces) {
                value.add(String.format("\"%s\"", impl.toString()));
            }
            e.addAttribute("extends", value);
        }
        if (context != null) {
            context.toJSON(e);
        }
        if (interfaceBlock != null) {
            for (JMember member : interfaceBlock) {
                ((JAST) member).toJSON(e);
            }
        }
    }
}
