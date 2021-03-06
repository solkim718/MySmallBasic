package com.coducation.smallbasic.codegen;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.coducation.smallbasic.ArithExpr;
import com.coducation.smallbasic.Array;
import com.coducation.smallbasic.Assign;
import com.coducation.smallbasic.BasicBlockEnv;
import com.coducation.smallbasic.BlockStmt;
import com.coducation.smallbasic.CompExpr;
import com.coducation.smallbasic.Expr;
import com.coducation.smallbasic.ExprStmt;
import com.coducation.smallbasic.ForStmt;
import com.coducation.smallbasic.GotoStmt;
import com.coducation.smallbasic.IfStmt;
import com.coducation.smallbasic.InterpretException;
import com.coducation.smallbasic.Label;
import com.coducation.smallbasic.Lit;
import com.coducation.smallbasic.LogicalExpr;
import com.coducation.smallbasic.MethodCallExpr;
import com.coducation.smallbasic.ParenExpr;
import com.coducation.smallbasic.PropertyExpr;
import com.coducation.smallbasic.Stmt;
import com.coducation.smallbasic.SubCallExpr;
import com.coducation.smallbasic.SubDef;
import com.coducation.smallbasic.Var;
import com.coducation.smallbasic.WhileStmt;

public class GenJava {

	StringBuilder globalVar;
	StringBuilder topLevel;
	//ArrayList<Pair<String, StringBuilder>> method;
	LinkedHashMap<String, StringBuilder> methods;
	String currentMethod;
	String emptyMethod;
	StringBuilder currentMethodBody;
	int numberOfIndent;
	HashMap<String, Stmt> trees;
	private static String[] programArgs;
	static String className;
	static String fileName;
	static ArrayList<String> idx_s;

	public GenJava(BasicBlockEnv bbenv, String[] args) {
		this();
		trees = bbenv.getMap();
		programArgs = args;
	}

	public GenJava() {
		globalVar = new StringBuilder("");
		topLevel = new StringBuilder("");
		methods = new LinkedHashMap<String, StringBuilder>();
		idx_s = new ArrayList<String>();
	}

	public String printIndent() {
		StringBuilder javaIndent = new StringBuilder("");

		for (int i = 0; i <= numberOfIndent; i++) {
			javaIndent.append("    ");
		}

		return javaIndent.toString();
	}

	public static void main(String[] args) {
		GenJava g = new GenJava();
	}

	//args[0] : Smallbasic file name
	//stmt : 스몰베이직의 AST
	public void codeGen(String[] args) {
		fileName = args[0].split("/")[args[0].split("/").length - 1];
		className = fileName.substring(0,1).toUpperCase() + fileName.substring(1, fileName.length()-3);

		Set<Map.Entry<String, Stmt>> set = trees.entrySet();
		for (Map.Entry<String, Stmt> entry : set) {
			if(entry.getKey().contains("$"))
				currentMethod = entry.getKey().substring(1); // tree --> method
			else currentMethod = entry.getKey();
			Stmt stmt = entry.getValue();

			if((stmt instanceof BlockStmt)&&(((BlockStmt)stmt).getAL().size() == 0)) {
				emptyMethod = currentMethod;
			}
			else {
				methods.put(currentMethod, new StringBuilder("    public static void " + currentMethod + "() {\r\n")); // 처음
				numberOfIndent++;
				codeGen(false, stmt);
				if(methods.get(currentMethod) != null) 
					methods.put(currentMethod, methods.get(currentMethod).append("    }\r\n"));
				else methods.put(currentMethod, new StringBuilder("    }\r\n"));
				numberOfIndent--;
			}

		}

		if(emptyMethod != null) { // emptyMethod 제거
			Set<String> keySet = trees.keySet();
			for(String l : keySet) {
				String s = null;
				if(l.contains("$"))
					s = l.substring(1);
				else s = l;
				if(!s.equals(emptyMethod))
					methods.put(s, new StringBuilder(methods.get(s).toString().replace("env.label(" + className + "_C.getMethod(\"" + emptyMethod + "\", null))", "env.label(null)")));
			}
		}


		OutputStreamWriter osw;
		try {
			//스몰베이직파일명과 동일한 .java 파일을 오픈

			osw = new OutputStreamWriter(new FileOutputStream(args[0].substring(0, args[0].length()-fileName.length()) + className +".java"), "UTF-8");
			System.out.println(className);

			//1~9번까지 출력
			osw.write("public class " + className + " {\r\n");
			osw.write("\r\n");
			osw.write("    static Env env;\r\n");
			osw.write("    static final String lib = \"com.coducation.smallbasic.lib.\";\r\n");
			osw.write("    static final String notifyFieldAssign = \"notifyFieldAssign\";\r\n");
			osw.write("    static final String notifyFieldRead = \"notifyFieldRead\";\r\n");
			osw.write("    static final Class "+ className +"_C = getClass(\"" + className + "\");\r\n");
			osw.write("\r\n");
			osw.write(classEnvGen("    ")); // Env Class 생성
			osw.write("    public " + className +"() {\r\n");
			osw.write("        env = new Env();\r\n");
			osw.write("    }\r\n");
			osw.write("\r\n");
			osw.write(mainGen("    ")); // main Method 생성
			//스몰베이직의 각 서브루틴으로부터 생성된 자바메소드들을 출력
			Iterator<Entry<String, StringBuilder>> it = methods.entrySet().iterator();
			while(it.hasNext()) {
				osw.write(it.next().getValue().toString());
				osw.write("\r\n");
			}
			osw.write(assignVarGen("    "));
			osw.write(getVarGen("    "));
			osw.write(assignPropertyExprGen("    "));
			osw.write(getPropertyExprGen("    "));
			osw.write(assignArrayGen("    "));
			osw.write(getArrayGen("    "));
			osw.write(getClassGen("    "));
			osw.write(getListGen("    "));
			osw.write(isNumberGen("    "));
			//13번 출력
			osw.write("}\r\n");
			osw.flush();

			//파일을 닫기
			osw.close();

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	//스몰베이직 문장을 받아 자바문장에 대한 문자열을 만들고
	//topLevel 또는 methods에 추가
	public void codeGen(boolean isTopLevel, Stmt stmt) {
		if (stmt instanceof Assign)
			codeGen(isTopLevel, (Assign) stmt);
		else if (stmt instanceof BlockStmt)
			codeGen(isTopLevel, (BlockStmt) stmt);
		else if (stmt instanceof ExprStmt)
			codeGen(isTopLevel, (ExprStmt) stmt);
		else if (stmt instanceof ForStmt)
			codeGen(isTopLevel, (ForStmt) stmt);
		else if (stmt instanceof GotoStmt)
			codeGen(isTopLevel, (GotoStmt) stmt);
		else if (stmt instanceof IfStmt)
			codeGen(isTopLevel, (IfStmt) stmt);
		else if (stmt instanceof Label)
			codeGen(isTopLevel, (Label) stmt);
		else if (stmt instanceof SubDef)
			codeGen(isTopLevel, (SubDef) stmt);
		else if (stmt instanceof SubCallExpr)
			codeGen(isTopLevel, (SubCallExpr) stmt);
		else if (stmt instanceof WhileStmt)
			codeGen(isTopLevel, (WhileStmt) stmt);
		else
			throw new CodeGenException("Syntax Error!" + stmt.getClass());

	}

	public void codeGen(boolean isTopLevel, Assign assignStmt) {
		Expr lhs = assignStmt.getLSide();
		Expr rhs = assignStmt.getRSide();

		StringBuilder javaStmt = new StringBuilder("");
		javaStmt.append(printIndent());

		if(lhs instanceof Var) {
			Var var = (Var)lhs;
			javaStmt.append("assignVar(\"" + var.getVarName() + "\", (" + codeGen(rhs) + " + \"\"));\r\n");

		}
		else if(lhs instanceof PropertyExpr) {
			PropertyExpr propertyExpr = (PropertyExpr)lhs;
			javaStmt.append("assignPropertyExpr(\"" + propertyExpr.getObj() + "\", \"" + propertyExpr.getName() + "\", (" + codeGen(rhs) + " + \"\"));\r\n");

		}
		else if(lhs instanceof Array) {
			Array arr = (Array) lhs;
			idx_s.clear();

			for (int i = 0; i < arr.getDim(); i++) {
				Expr idx = arr.getIndex(i);
				String s_idx = codeGen(idx);
				//Value v = Eval.eval(env, idx);

				if (s_idx == null || s_idx.equals(""))
					idx_s.add("0");
				else if (s_idx instanceof String) {
					idx_s.add(s_idx);
				}
				else {
					throw new CodeGenException("Unexpected Index" + idx_s);
				}

				/*if (v == null || v.toString().trim().equals(""))
					idx_s.add("0");
				else if (v instanceof StrV || v instanceof DoubleV) {
					idx_s.add(v.toString());
				} else {
					throw new CodeGenException("Unexpected Index" + v);
				}*/
			}

			javaStmt.append("assignArray(\"" + arr.getVar() + "\", (");
			javaStmt.append(codeGen(rhs) + " + \"\")");
			for(int i=0;i<idx_s.size();i++) {
				javaStmt.append(", " + idx_s.get(i) + " + \"\"");
			}
			javaStmt.append(");\r\n");
		}
		else {
			throw new CodeGenException("Assign : Unknown lhs " + lhs);
		}

		if(isTopLevel) {
			topLevel.append(javaStmt);
		}
		else {
			if(methods.get(currentMethod) != null) 
				methods.put(currentMethod, methods.get(currentMethod).append(javaStmt));
			else methods.put(currentMethod, javaStmt);
		}

		/*if(isTopLevel) {
			if(lhs instanceof Var || lhs instanceof Array) {
				topLevel.append(javaStmt);
			}
			else {
				topLevel.append(javaStmt);
			}

		}
		else {
			if(methods.get(currentMethod) != null) 
				methods.put(currentMethod, methods.get(currentMethod).append(javaStmt));
			else methods.put(currentMethod, javaStmt);
		}*/
	}

	public void codeGen(boolean isTopLevel, BlockStmt blockStmt) {
		numberOfIndent++;

		for (int i = 0; i < blockStmt.getAL().size(); i++) {
			codeGen(isTopLevel, blockStmt.getAL().get(i));
		}

		numberOfIndent--;
	}

	public void codeGen(boolean isTopLevel, ExprStmt exprStmt) {
		StringBuilder javaStmt = new StringBuilder("");
		javaStmt.append(printIndent());

		javaStmt.append(codeGen(exprStmt.getExpr()));
		javaStmt.append(";\r\n");

		if(isTopLevel) {
			topLevel.append(javaStmt);
		}
		else {
			if(methods.get(currentMethod) != null) 
				methods.put(currentMethod, methods.get(currentMethod).append(javaStmt));
			else methods.put(currentMethod, javaStmt);
		}

	}

	public void codeGen(boolean isTopLevel, ForStmt forStmt) {

		StringBuilder javaStmt = new StringBuilder(printIndent());

		javaStmt.append("for(int ");
		javaStmt.append(codeGen(forStmt.getVar()));
		javaStmt.append(" = ");
		javaStmt.append(codeGen(forStmt.getInit()));
		javaStmt.append(" ; ");
		javaStmt.append(codeGen(forStmt.getVar()));
		javaStmt.append(" <= ");
		javaStmt.append(codeGen(forStmt.getEnd()));
		javaStmt.append(" ; ");

		if (forStmt.getStep() != null) {
			javaStmt.append(codeGen(forStmt.getVar()));
			javaStmt.append(" = ");
			javaStmt.append(codeGen(forStmt.getVar()));
			javaStmt.append(codeGen(forStmt.getStep()));
		}
		else {
			javaStmt.append(codeGen(forStmt.getVar()));
			javaStmt.append("++");
		}
		javaStmt.append(") {\r\n");

		if(isTopLevel) {
			topLevel.append(javaStmt);
		}
		else {
			if(methods.get(currentMethod) != null) 
				methods.put(currentMethod, methods.get(currentMethod).append(javaStmt));
			else methods.put(currentMethod, javaStmt);
		}
		javaStmt = new StringBuilder(printIndent());

		codeGen(isTopLevel, forStmt.getBlock());


		javaStmt.append("}\r\n");

		if(isTopLevel) {
			topLevel.append(javaStmt);
		}
		else {
			if(methods.get(currentMethod) != null) 
				methods.put(currentMethod, methods.get(currentMethod).append(javaStmt));
			else methods.put(currentMethod, javaStmt);
		}
	}

	public void codeGen(boolean isTopLevel, GotoStmt gotoStmt) { // subCall role
		StringBuilder javaStmt = new StringBuilder(printIndent());

		javaStmt.append("env.label(" + className + "_C.getMethod(\"" + gotoStmt.getTargetLabel().substring(1) + "\", null));\r\n");
		//javaStmt.append(className + "." + gotoStmt.getTargetLabel().substring(1) + "();\r\n");

		if(isTopLevel) {
			topLevel.append(javaStmt);
		}
		else {
			if(methods.get(currentMethod) != null) 
				methods.put(currentMethod, methods.get(currentMethod).append(javaStmt));
			else methods.put(currentMethod, javaStmt);
		}
	}

	public void codeGen(boolean isTopLevel, IfStmt ifStmt) {

		StringBuilder javaStmt = new StringBuilder(printIndent());

		javaStmt.append("if(");
		javaStmt.append(codeGen(ifStmt.getCond()));
		javaStmt.append(") {\r\n");

		if(isTopLevel) {
			topLevel.append(javaStmt);
		}
		else {
			if(methods.get(currentMethod) != null) 
				methods.put(currentMethod, methods.get(currentMethod).append(javaStmt));
			else methods.put(currentMethod, javaStmt);
		}
		javaStmt = new StringBuilder(printIndent());

		codeGen(isTopLevel, ifStmt.getThen());
		javaStmt.append("}\r\n");

		if(isTopLevel) {
			topLevel.append(javaStmt);
		}
		else {
			if(methods.get(currentMethod) != null) 
				methods.put(currentMethod, methods.get(currentMethod).append(javaStmt));
			else methods.put(currentMethod, javaStmt);
		}
		javaStmt = new StringBuilder(printIndent());

		if (ifStmt.getElse() != null) {
			printIndent();

			javaStmt.append("else {\r\n");

			if(isTopLevel) {
				topLevel.append(javaStmt);
			}
			else {
				if(methods.get(currentMethod) != null) 
					methods.put(currentMethod, methods.get(currentMethod).append(javaStmt));
				else methods.put(currentMethod, javaStmt);
			}
			javaStmt = new StringBuilder(printIndent());

			codeGen(isTopLevel, ifStmt.getElse());
			javaStmt.append("}\r\n");

			if(isTopLevel) {
				topLevel.append(javaStmt);
			}
			else {
				if(methods.get(currentMethod) != null) 
					methods.put(currentMethod, methods.get(currentMethod).append(javaStmt));
				else methods.put(currentMethod, javaStmt);
			}
		}

	}

	public void codeGen(boolean isTopLevel, Label labelStmt) {

		StringBuilder javaStmt = new StringBuilder(printIndent());

		javaStmt.append(labelStmt.getLabel() + ":\r\n");

		if(isTopLevel) {
			topLevel.append(javaStmt);
		}
		else {
			if(methods.get(currentMethod) != null) 
				methods.put(currentMethod, methods.get(currentMethod).append(javaStmt));
			else methods.put(currentMethod, javaStmt);
		}
	}

	public void codeGen(boolean isTopLevel, SubDef subDefStmt) {
		throw new CodeGenException("SubDef : Unexpected");
	}

	public void codeGen(boolean isTopLevel, SubCallExpr subCallExpr) {

		StringBuilder javaStmt = new StringBuilder(printIndent());

		javaStmt.append(className + "." + subCallExpr.getName() + "();\r\n");

		if(isTopLevel) {
			topLevel.append(javaStmt);
		}
		else {
			if(methods.get(currentMethod) != null) 
				methods.put(currentMethod, methods.get(currentMethod).append(javaStmt));
			else methods.put(currentMethod, javaStmt);
		}
	}

	public void codeGen(boolean isTopLevel, WhileStmt whileStmt) {

		StringBuilder javaStmt = new StringBuilder(printIndent());

		javaStmt.append("While(");
		javaStmt.append(codeGen(whileStmt.getCond()));
		javaStmt.append(") {\r\n");

		if(isTopLevel) {
			topLevel.append(javaStmt);
		}
		else {
			if(methods.get(currentMethod) != null) 
				methods.put(currentMethod, methods.get(currentMethod).append(javaStmt));
			else methods.put(currentMethod, javaStmt);
		}
		javaStmt = new StringBuilder(printIndent());

		codeGen(isTopLevel, whileStmt.getBlock());

		javaStmt.append("}\r\n");
		if(isTopLevel) {
			topLevel.append(javaStmt);
		}
		else {
			if(methods.get(currentMethod) != null) 
				methods.put(currentMethod, methods.get(currentMethod).append(javaStmt));
			else methods.put(currentMethod, javaStmt);
		}
	}

	//스몰베이직 식을 받아 자바식에 대한 문자열을 만들어 리턴
	public String codeGen(Expr expr) {

		if (expr instanceof ArithExpr)
			return codeGen((ArithExpr) expr);
		else if (expr instanceof Array)
			return codeGen((Array) expr);
		else if (expr instanceof CompExpr)
			return codeGen((CompExpr) expr);
		else if (expr instanceof Lit)
			return codeGen((Lit) expr);
		else if (expr instanceof LogicalExpr)
			return codeGen((LogicalExpr) expr);
		else if (expr instanceof MethodCallExpr)
			return codeGen((MethodCallExpr) expr);
		else if (expr instanceof ParenExpr)
			return codeGen((ParenExpr) expr);
		else if (expr instanceof PropertyExpr)
			return codeGen((PropertyExpr) expr);
		else if (expr instanceof Var)
			return codeGen((Var) expr);
		else
			throw new InterpretException("Syntax Error! " + expr.getClass());

	}

	public String codeGen(ArithExpr arithExpr) {

		StringBuilder javaExpr = new StringBuilder("");

		switch (arithExpr.GetOp()) {
		case 1:
			javaExpr.append(codeGen(arithExpr.GetOperand()[0]));
			javaExpr.append(" + ");
			break;
		case 2:
			javaExpr.append(codeGen(arithExpr.GetOperand()[0]));
			javaExpr.append(" - ");
			break;
		case 3:
			javaExpr.append(codeGen(arithExpr.GetOperand()[0]));
			javaExpr.append(" * ");
			break;
		case 4:
			javaExpr.append(codeGen(arithExpr.GetOperand()[0]));
			javaExpr.append(" / ");
			break;
		case 5:
			javaExpr.append("- ");
			javaExpr.append(codeGen(arithExpr.GetOperand()[0]));
			break;
		}
		if (arithExpr.GetOperand()[1] != null)
			javaExpr.append(codeGen(arithExpr.GetOperand()[1]));

		return javaExpr.toString();
	}

	public String codeGen(Array arrayExpr) {

		StringBuilder javaExpr = new StringBuilder("");
		idx_s.clear();

		for (int i = 0; i < arrayExpr.getDim(); i++) {
			Expr idx = arrayExpr.getIndex(i);
			String s_idx = codeGen(idx);
			//Value v = Eval.eval(env, idx);

			if (s_idx == null || s_idx.equals(""))
				idx_s.add("0");
			else if (s_idx instanceof String) {
				idx_s.add(s_idx);
			}
			else {
				throw new CodeGenException("Unexpected Index" + idx_s);
			}
		}

		javaExpr.append("getArray(\"" + arrayExpr.getVar() + "\"");
		for(int i=0;i<idx_s.size();i++) {
			javaExpr.append(", \"" + idx_s.get(i) + "\"" );
		}
		javaExpr.append(")");

		/*for (int i = 0; i < arrayExpr.getDim(); i++) {
			javaExpr.append("[" + arrayExpr.getIndex(i) + "]");
		}*/

		return "(isNumber(" + javaExpr.toString() + ".toString())? Double.parseDouble(" + javaExpr.toString() + ".toString()): " + javaExpr.toString() + ".toString())";
	}

	public String codeGen(CompExpr compExpr) {

		StringBuilder javaExpr = new StringBuilder("");
		javaExpr.append(codeGen(compExpr.GetOperand()[0]));

		switch (compExpr.GetOp()) {
		case CompExpr.GREATER_THAN:
			javaExpr.append(" > ");
			break;
		case CompExpr.LESS_THAN:
			javaExpr.append(" < ");
			break;
		case CompExpr.GREATER_EQUAL:
			javaExpr.append(" >= ");
			break;
		case CompExpr.LESS_EQUAL:
			javaExpr.append(" <= ");
			break;
		case CompExpr.EQUAL:
			javaExpr.append(" = ");
			break;
		case CompExpr.NOT_EQUAL:
			javaExpr.append(" <> ");
			break;
		default:
			System.err.println("Unknown CompExpr Operator " + compExpr.GetOp());
			break;
		}
		javaExpr.append(codeGen(compExpr.GetOperand()[1]));

		return javaExpr.toString();
	}

	public String codeGen(Lit litExpr) {
		String javaExpr = litExpr.gets();
		String r_javaExpr = javaExpr.replaceAll("\"", "");
		if(isNumber(r_javaExpr))
			return r_javaExpr;
		else return javaExpr;
	}

	public String codeGen(LogicalExpr logicalExpr) {

		StringBuilder javaExpr = new StringBuilder("");
		javaExpr.append(codeGen(logicalExpr.GetOperand()[0]));

		switch (logicalExpr.GetOp()) {
		case 1:
			javaExpr.append(" && ");
			break;
		case 2:
			javaExpr.append(" || ");
			break;
		default:
			System.err.println("Unknown Logical Operator " + logicalExpr.GetOp());
			break;
		}
		javaExpr.append(codeGen(logicalExpr.GetOperand()[1]));

		return javaExpr.toString();
	}

	public String codeGen(MethodCallExpr methodCallExpr) {

		StringBuilder javaExpr = new StringBuilder("");
		javaExpr.append(methodCallExpr.getObj() + "." + methodCallExpr.getName() + "(");
		javaExpr.append("getList(");

		if (methodCallExpr.getArgs() != null) {
			int size = methodCallExpr.getArgs().size();
			for (int i = 0; i < size; i++) {
				javaExpr.append("new StrV(" + codeGen(methodCallExpr.getArgs().get(i)) + " + \"\")");
				if (i != size - 1)
					javaExpr.append(", ");
			}
		}
		javaExpr.append(")");
		javaExpr.append(")");

		return javaExpr.toString();

	}

	public String codeGen(ParenExpr parenExpr) {

		return "(" + codeGen(parenExpr.get()) + ")";
	}

	public String codeGen(PropertyExpr propertyExpr) {
		StringBuilder javaExpr = new StringBuilder("");
		javaExpr.append("getPropertyExpr(\"" + propertyExpr.getObj() + "\", \"" + propertyExpr.getName() + "\")");

		return "(isNumber(" + javaExpr.toString() + ".toString())? " + javaExpr.toString() + ".getNumber(): " + javaExpr.toString() + ".toString())";
	}

	public String codeGen(Var var) {
		if (trees.get(var.getVarName()) != null)
			return var.getVarName();
		else
			return "(isNumber(getVar(\"" + var.getVarName() + "\").toString())?getVar(\"" + var.getVarName() + "\").getNumber():getVar(\"" + var.getVarName() + "\").toString())";
	}
	
	public static String classEnvGen(String indent) {
		StringBuilder javaStmt = new StringBuilder("");

		javaStmt.append(indent);
		javaStmt.append("class Env extends com.coducation.smallbasic.Env {\r\n");
		javaStmt.append(indent);
		javaStmt.append("    private HashMap<String,Method> labels;\r\n");
		javaStmt.append(indent);
		javaStmt.append("    private static final String label = \"$label\";\r\n");
		javaStmt.append("\r\n");
		javaStmt.append(indent);
		javaStmt.append("    public Method label() {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        return labels.get(label + Thread.currentThread().getId());\r\n");
		javaStmt.append(indent);
		javaStmt.append("    }\r\n");
		javaStmt.append("\r\n");
		javaStmt.append(indent);
		javaStmt.append("    public void label(Method _label) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        labels.put(label + Thread.currentThread().getId(), _label);\r\n");
		javaStmt.append(indent);
		javaStmt.append("    }\r\n");
		javaStmt.append(indent);
		javaStmt.append("}\r\n");
		javaStmt.append("\r\n");

		return javaStmt.toString();
	}

	public static String mainGen(String indent) {
		StringBuilder javaStmt = new StringBuilder("");

		javaStmt.append(indent);
		javaStmt.append("public static void main(String[] args) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("    Method m = " + className + "_C.getMethod(\"main\");\r\n");
		javaStmt.append(indent);
		javaStmt.append("    env.label(m);\r\n");
		javaStmt.append("\r\n");
		javaStmt.append(indent);
		javaStmt.append("    while (env.label() != null) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        env.label(null);\r\n");
		javaStmt.append(indent);
		javaStmt.append("        m.invoke(null);\r\n");
		javaStmt.append(indent);
		javaStmt.append("    }\r\n");
		javaStmt.append(indent);
		javaStmt.append("}\r\n");
		javaStmt.append("\r\n");

		return javaStmt.toString();
	}

	public static String getClassGen(String indent) {
		StringBuilder javaStmt = new StringBuilder("");

		javaStmt.append(indent);
		javaStmt.append("public static Class getClass(String name) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("    try {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        return Class.forName(lib + name);\r\n");
		javaStmt.append(indent);
		javaStmt.append("    } catch (ClassNotFoundException e) {;\r\n");
		javaStmt.append(indent);
		javaStmt.append("        throw new CodeGenException(\"Class Not Found \" + e.toString());\r\n");
		javaStmt.append(indent);
		javaStmt.append("    }\r\n");
		javaStmt.append(indent);
		javaStmt.append("}\r\n");
		javaStmt.append("\r\n");

		return javaStmt.toString();
	}

	public static String assignVarGen(String indent) {
		StringBuilder javaStmt = new StringBuilder("");

		javaStmt.append(indent);
		javaStmt.append("public static void assignVar(String varName, String rhsValue) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("    if(isNumber(rhsValue))\r\n");
		javaStmt.append(indent);
		javaStmt.append("        env.put(varName, new DoubleV(Double.parseDouble(rhsValue)));\r\n");
		javaStmt.append(indent);
		javaStmt.append("    else\r\n");
		javaStmt.append(indent);
		javaStmt.append("        env.put(varName, new StrV(rhsValue));\r\n");
		javaStmt.append(indent);
		javaStmt.append("}\r\n");
		javaStmt.append("\r\n");

		return javaStmt.toString();
	}

	public static String getVarGen(String indent) {
		StringBuilder javaStmt = new StringBuilder("");

		javaStmt.append(indent);
		javaStmt.append("public static Value getVar(String varName) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("    return env.get(varName);\r\n");
		javaStmt.append(indent);
		javaStmt.append("}\r\n");
		javaStmt.append("\r\n");

		return javaStmt.toString();
	}

	public static String assignPropertyExprGen(String indent) {
		StringBuilder javaStmt = new StringBuilder("");

		javaStmt.append(indent);
		javaStmt.append("public static void assignPropertyExpr(String lhsObj, String lhsName, String rhsValue) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("    try {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        String clzName = lhsObj;\r\n");
		javaStmt.append(indent);
		javaStmt.append("        Class clz = getClass(clzName);\r\n");
		javaStmt.append(indent);
		javaStmt.append("        Field fld = clz.getField(lhsName);\r\n");
		javaStmt.append(indent);
		javaStmt.append("    if(isNumber(rhsValue))\r\n");
		javaStmt.append(indent);
		javaStmt.append("        fld.set(null, new DoubleV(Double.parseDouble(rhsValue)));\r\n");
		javaStmt.append(indent);
		javaStmt.append("    else\r\n");
		javaStmt.append(indent);
		javaStmt.append("        fld.set(null, new StrV(rhsValue));\r\n");
		javaStmt.append(indent);
		javaStmt.append("        Method mth = clz.getMethod(notifyFieldAssign, String.class);\r\n");
		javaStmt.append(indent);
		javaStmt.append("        mth.invoke(null, lhsName);\r\n");
		javaStmt.append(indent);
		javaStmt.append("    } catch (NoSuchFieldException | SecurityException e) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        throw new CodeGenException(\"Assign : \" + e.toString());\r\n");
		javaStmt.append(indent);
		javaStmt.append("    } catch (IllegalArgumentException e) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        throw new CodeGenException(\"Assign : \" + e.toString());\r\n");
		javaStmt.append(indent);
		javaStmt.append("    } catch (IllegalAccessException e) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        throw new CodeGenException(\"Assign : \" + e.toString());\r\n");
		javaStmt.append(indent);
		javaStmt.append("    } catch (NoSuchMethodException e) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        throw new CodeGenException(\"Method Not Found \" + e.toString());\r\n");
		javaStmt.append(indent);
		javaStmt.append("    } catch (InvocationTargetException e) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        throw new CodeGenException(\"Target Not Found \" + e.toString() + \": \");\r\n");
		javaStmt.append(indent);
		javaStmt.append("    }\r\n");
		javaStmt.append(indent);
		javaStmt.append("}\r\n");
		javaStmt.append("\r\n");

		return javaStmt.toString();
	}

	public static String getPropertyExprGen(String indent) {
		StringBuilder javaStmt = new StringBuilder("");

		javaStmt.append(indent);
		javaStmt.append("public static Value getPropertyExpr(String obj, String name) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("    try {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        String clzName = obj;\r\n");
		javaStmt.append(indent);
		javaStmt.append("        Class clz = getClass(clzName);\r\n");
		javaStmt.append(indent);
		javaStmt.append("        Field fld = clz.getField(name);\r\n");
		javaStmt.append(indent);
		javaStmt.append("        Method mth = clz.getMethod(notifyFieldAssign, String.class);\r\n");
		javaStmt.append(indent);
		javaStmt.append("        mth.invoke(null, name);\r\n");
		javaStmt.append(indent);
		javaStmt.append("        return (Value) fld.get(null);\r\n");
		javaStmt.append(indent);
		javaStmt.append("    } catch (NoSuchFieldException | SecurityException e) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        throw new CodeGenException(\"PropertyExpr : \" + e.toString());\r\n");
		javaStmt.append(indent);
		javaStmt.append("    } catch (IllegalArgumentException e) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        throw new CodeGenException(\"PropertyExpr : \" + e.toString());\r\n");
		javaStmt.append(indent);
		javaStmt.append("    } catch (IllegalAccessException e) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        throw new CodeGenException(\"PropertyExpr : \" + e.toString());\r\n");
		javaStmt.append(indent);
		javaStmt.append("    } catch (NoSuchMethodException e) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        throw new CodeGenException(\"Method Not Found \" + e.toString());\r\n");
		javaStmt.append(indent);
		javaStmt.append("    } catch (InvocationTargetException e) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        throw new CodeGenException(\"Target Not Found \" + e.toString() + \": \");\r\n");
		javaStmt.append(indent);
		javaStmt.append("    }\r\n");
		javaStmt.append(indent);
		javaStmt.append("}\r\n");
		javaStmt.append("\r\n");

		return javaStmt.toString();
	}

	public static String assignArrayGen(String indent) {
		StringBuilder javaStmt = new StringBuilder("");

		javaStmt.append(indent);
		javaStmt.append("public static void assignArray(String arrayName, String rhsValue, String... idx_s) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("    Value arrValue = env.get(arrayName);\r\n");
		javaStmt.append(indent);
		javaStmt.append("    ArrayV elem;\r\n");
		javaStmt.append("\r\n");
		javaStmt.append(indent);
		javaStmt.append("    if (arrValue == null)\r\n");
		javaStmt.append(indent);
		javaStmt.append("        elem = null;\r\n");
		javaStmt.append(indent);
		javaStmt.append("    else if (arrValue instanceof ArrayV)\r\n");
		javaStmt.append(indent);
		javaStmt.append("        elem = (ArrayV) arrValue;\r\n");
		javaStmt.append(indent);
		javaStmt.append("    else {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        elem = null;\r\n");
		javaStmt.append(indent);
		javaStmt.append("    }\r\n");
		javaStmt.append("\r\n");
		javaStmt.append(indent);
		javaStmt.append("    if (elem == null) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        elem = new ArrayV();\r\n");
		javaStmt.append(indent);
		javaStmt.append("        env.put(arrayName, elem);\r\n");
		javaStmt.append(indent);
		javaStmt.append("    }\r\n");
		javaStmt.append("\r\n");
		javaStmt.append(indent);
		javaStmt.append("    for (int i = 0; i < idx_s.length; i++) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        if (i < idx_s.length - 1) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("            ArrayV elem_elem = (ArrayV) elem.get(idx_s[i]);\r\n");
		javaStmt.append(indent);
		javaStmt.append("            if (elem_elem == null) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("                elem_elem = new ArrayV();\r\n");
		javaStmt.append(indent);
		javaStmt.append("                elem.put(idx_s[i], elem_elem);\r\n");
		javaStmt.append(indent);
		javaStmt.append("            }\r\n");
		javaStmt.append(indent);
		javaStmt.append("            elem = elem_elem;\r\n");
		javaStmt.append(indent);
		javaStmt.append("        } else {\r\n");
		javaStmt.append(indent);
		javaStmt.append("    if(isNumber(rhsValue))\r\n");
		javaStmt.append(indent);
		javaStmt.append("            elem.put(idx_s[i], new DoubleV(Double.parseDouble(rhsValue)));\r\n");
		javaStmt.append(indent);
		javaStmt.append("    else\r\n");
		javaStmt.append(indent);
		javaStmt.append("            elem.put(idx_s[i], new StrV(rhsValue));\r\n");
		javaStmt.append(indent);
		javaStmt.append("        }\r\n");
		javaStmt.append(indent);
		javaStmt.append("    }\r\n");
		javaStmt.append(indent);
		javaStmt.append("}\r\n");
		javaStmt.append("\r\n");

		return javaStmt.toString();
	}

	public static String getArrayGen(String indent) {
		StringBuilder javaStmt = new StringBuilder("");

		javaStmt.append(indent);
		javaStmt.append("public static Value getArray(String arrayName, String... idx_s) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("    ArrayV arrV;\r\n");
		javaStmt.append(indent);
		javaStmt.append("    Value elem = null;\r\n");
		javaStmt.append("\r\n");
		javaStmt.append(indent);
		javaStmt.append("    if (env.get(arrayName) instanceof ArrayV) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        arrV = (ArrayV) env.get(arrayName);\r\n");
		javaStmt.append(indent);
		javaStmt.append("        elem = arrV;\r\n");
		javaStmt.append(indent);
		javaStmt.append("    }\r\n");
		javaStmt.append("\r\n");
		javaStmt.append(indent);
		javaStmt.append("    for (int i = 0; i < idx_s.length; i++) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        if (elem == null)\r\n");
		javaStmt.append(indent);
		javaStmt.append("            break;\r\n");
		javaStmt.append(indent);
		javaStmt.append("        else if (elem instanceof StrV) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("        } else\r\n");
		javaStmt.append(indent);
		javaStmt.append("            elem = ((ArrayV) elem).get(idx_s[i]);\r\n");
		javaStmt.append(indent);
		javaStmt.append("    }\r\n");
		javaStmt.append(indent);
		javaStmt.append("    if (elem == null)\r\n");
		javaStmt.append(indent);
		javaStmt.append("        return new StrV(\"\");\r\n");
		javaStmt.append(indent);
		javaStmt.append("    else\r\n");
		javaStmt.append(indent);
		javaStmt.append("        return elem;\r\n");
		javaStmt.append(indent);
		javaStmt.append("}\r\n");
		javaStmt.append("\r\n");

		return javaStmt.toString();
	}
	
	public static String getListGen(String indent) {
		StringBuilder javaStmt = new StringBuilder("");

		javaStmt.append(indent);
		javaStmt.append("public static ArrayList<Value> getList(StrV... args) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("    ArrayList<Value> list = new ArrayList<Value>();\r\n");
		javaStmt.append(indent);
		javaStmt.append("    Collections.addAll(list, args);\r\n");
		javaStmt.append(indent);
		javaStmt.append("    return list;\r\n");
		javaStmt.append(indent);
		javaStmt.append("}\r\n");
		javaStmt.append("\r\n");

		return javaStmt.toString();
	}
	
	public static String isNumberGen(String indent) {
		StringBuilder javaStmt = new StringBuilder("");

		javaStmt.append(indent);
		javaStmt.append("public static boolean isNumber(String v) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("    try {\r\n");
		javaStmt.append(indent);
		javaStmt.append("    Double.parseDouble(v);\r\n");
		javaStmt.append(indent);
		javaStmt.append("    return true;\r\n");
		javaStmt.append(indent);
		javaStmt.append("    } catch(NumberFormatException e) {\r\n");
		javaStmt.append(indent);
		javaStmt.append("    return false;\r\n");
		javaStmt.append(indent);
		javaStmt.append("    }\r\n");
		javaStmt.append(indent);
		javaStmt.append("}\r\n");
		javaStmt.append("\r\n");

		return javaStmt.toString();
	}
	
	public static boolean isNumber(String s) {
		try {
			Double.parseDouble(s);
			return true;
		} catch(NumberFormatException e) {
			return false;
		}
	}


}
