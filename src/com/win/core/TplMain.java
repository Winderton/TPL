package com.win.core;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TplMain {
	    public static void main(String[] args) {
	        
	        if (args.length != 1) {
	            System.out.println("Использование: tpl <script>");
	            System.out.println("<script> это относительный путь к .tpl скриптам.");
	            return;
	        }
	        
	        
	        String contents = readFile(args[0]);
	        
	        
	        TplMain TplMain = new TplMain(); 
	        TplMain.interpret(contents);
	    }
	    
	  
	    private static List<Token> lex(String src) {
	        List<Token> tokens = new ArrayList<Token>();
	        
	        String token = "";
	        lexState state = lexState.DEFAULT;
	        
	        
	        String charTokens = "\n=+-*/<>()";
	        TokenType[] tokenTypes = { TokenType.LINE, TokenType.EQUALS,
	            TokenType.op, TokenType.op, TokenType.op,
	            TokenType.op, TokenType.op, TokenType.op,
	            TokenType.l_PARENT, TokenType.r_PARENT
	        };
	        
	        for (int i = 0; i < src.length(); i++) {
	            char c = src.charAt(i);
	            switch (state) {
	            case DEFAULT:
	                if (charTokens.indexOf(c) != -1) {
	                    tokens.add(new Token(Character.toString(c),
	                        tokenTypes[charTokens.indexOf(c)]));
	                } else if (Character.isLetter(c)) {
	                    token += c;
	                    state = lexState.WORD;
	                } else if (Character.isDigit(c)) {
	                    token += c;
	                    state = lexState.NUMBER;
	                } else if (c == '"') {
	                    state = lexState.STRING;
	                } else if (c == '\'') {
	                    state = lexState.COMMENT;
	                }
	                break;
	                
	            case WORD:
	                if (Character.isLetterOrDigit(c)) {
	                    token += c;
	                } else if (c == ':') {
	                    tokens.add(new Token(token, TokenType.LBL));
	                    token = "";
	                    state = lexState.DEFAULT;
	                } else {
	                    tokens.add(new Token(token, TokenType.WORD));
	                    token = "";
	                    state = lexState.DEFAULT;
	                    i--; 
	                }
	                break;
	                
	            case NUMBER:
	                
	                if (Character.isDigit(c)) {
	                    token += c;
	                } else {
	                    tokens.add(new Token(token, TokenType.NUMBER));
	                    token = "";
	                    state = lexState.DEFAULT;
	                    i--; 
	                }
	                break;
	                
	            case STRING:
	                if (c == '"') {
	                    tokens.add(new Token(token, TokenType.STRING));
	                    token = "";
	                    state = lexState.DEFAULT;
	                } else {
	                    token += c;
	                }
	                break;
	                
	            case COMMENT:
	                if (c == '\n') {
	                    state = lexState.DEFAULT;
	                }
	                break;
	            }
	        }
	        
	       
	        return tokens;
	    }

	    

	   
	    private enum TokenType {
	        WORD, NUMBER, STRING, LBL, LINE,
	        EQUALS, op, l_PARENT, r_PARENT, EOF
	    }
	    
	    
	    private static class Token {
	        public Token(String text, TokenType type) {
	            this.text = text;
	            this.type = type;
	        }
	        
	        public final String text;
	        public final TokenType type;
	    }
	    
	    
	    private enum lexState {
	        DEFAULT, WORD, NUMBER, STRING, COMMENT
	    }

	    
	    private class Parser {
	        public Parser(List<Token> tokens) {
	            this.tokens = tokens;
	            position = 0;
	        }
	        
	        
	        public List<Statement> parse(Map<String, Integer> LBLs) {
	            List<Statement> statements = new ArrayList<Statement>();
	            
	            while (true) {
	                
	                while (match(TokenType.LINE));
	                
	                if (match(TokenType.LBL)) {
	                    
	                    LBLs.put(last(1).text, statements.size());
	                } else if (match(TokenType.WORD, TokenType.EQUALS)) {
	                    String name = last(2).text;
	                    Expression value = expression();
	                    statements.add(new AssignStatement(name, value));
	                } else if (match("print")) {
	                    statements.add(new PrintStatement(expression()));
	                } else if (match("input")) {
	                    statements.add(new InputStatement(
	                        consume(TokenType.WORD).text));
	                } else if (match("goto")) {
	                    statements.add(new GotoStatement(
	                        consume(TokenType.WORD).text));
	                } else if (match("if")) {
	                    Expression condition = expression();
	                    consume("then");
	                    String LBL = consume(TokenType.WORD).text;
	                    statements.add(new IfThenStatement(condition, LBL));
	                } else break; 
	            }
	            
	            return statements;
	        }
	        
	        
	        private Expression expression() {
	            return op();
	        }
	        
	       
	        private Expression op() {
	            Expression expression = atomic();
	            
	            
	            while (match(TokenType.op) ||
	                   match(TokenType.EQUALS)) {
	                char op = last(1).text.charAt(0);
	                Expression r = atomic();
	                expression = new opExpression(expression, op, r);
	            }
	            
	            return expression;
	        }
	        
	        
	        private Expression atomic() {
	            if (match(TokenType.WORD)) {
	                
	                return new VariableExpression(last(1).text);
	            } else if (match(TokenType.NUMBER)) {
	                return new NumVal(Double.parseDouble(last(1).text));
	            } else if (match(TokenType.STRING)) {
	                return new StringValue(last(1).text);
	            } else if (match(TokenType.l_PARENT)) {
	                
	                Expression expression = expression();
	                consume(TokenType.r_PARENT);
	                return expression;
	            }
	            throw new Error("Ошибка в парсинге :(");
	        }
	        
	        
	        private boolean match(TokenType type1, TokenType type2) {
	            if (get(0).type != type1) return false;
	            if (get(1).type != type2) return false;
	            position += 2;
	            return true;
	        }
	        
	        
	        private boolean match(TokenType type) {
	            if (get(0).type != type) return false;
	            position++;
	            return true;
	        }
	        
	       
	        private boolean match(String name) {
	            if (get(0).type != TokenType.WORD) return false;
	            if (!get(0).text.equals(name)) return false;
	            position++;
	            return true;
	        }
	        
	        
	        private Token consume(TokenType type) {
	            if (get(0).type != type) throw new Error("Нужно: " + type + ".");
	            return tokens.get(position++);
	        }
	        
	        
	        private Token consume(String name) {
	            if (!match(name)) throw new Error("Нужно: " + name + ".");
	            return last(1);
	        }

	       
	        private Token last(int offset) {
	            return tokens.get(position - offset);
	        }
	        
	        
	        private Token get(int offset) {
	            if (position + offset >= tokens.size()) {
	                return new Token("", TokenType.EOF);
	            }
	            return tokens.get(position + offset);
	        }
	        
	        private final List<Token> tokens;
	        private int position;
	    }
	    
	    
	    public interface Statement {
	        
	        void execute();
	    }

	    
	    public interface Expression {
	        
	        Value evaluate();
	    }
	    
	    
	    public class PrintStatement implements Statement {
	        public PrintStatement(Expression expression) {
	            this.expression = expression;
	        }
	        
	        public void execute() {
	            System.out.println(expression.evaluate().toString());
	        }

	        private final Expression expression;
	    }
	    
	    
	    public class InputStatement implements Statement {
	        public InputStatement(String name) {
	            this.name = name;
	        }
	        
	        public void execute() {
	            try {
	                String input = lineIn.readLine();
	                
	                
	                try {
	                    double value = Double.parseDouble(input);
	                    vars.put(name, new NumVal(value));
	                } catch (NumberFormatException e) {
	                    vars.put(name, new StringValue(input));
	                }
	            } catch (IOException e1) {
	                
	            }
	        }

	        private final String name;
	    }

	    
	    public class AssignStatement implements Statement {
	        public AssignStatement(String name, Expression value) {
	            this.name = name;
	            this.value = value;
	        }
	        
	        public void execute() {
	            vars.put(name, value.evaluate());
	        }

	        private final String name;
	        private final Expression value;
	    }
	    
	    
	    public class GotoStatement implements Statement {
	        public GotoStatement(String LBL) {
	            this.LBL = LBL;
	        }
	        
	        public void execute() {
	            if (LBLs.containsKey(LBL)) {
	                currentStatement = LBLs.get(LBL).intValue();
	            }
	        }

	        private final String LBL;
	    }
	    
	    
	    public class IfThenStatement implements Statement {
	        public IfThenStatement(Expression condition, String LBL) {
	            this.condition = condition;
	            this.LBL = LBL;
	        }
	        
	        public void execute() {
	            if (LBLs.containsKey(LBL)) {
	                double value = condition.evaluate().toNumber();
	                if (value != 0) {
	                    currentStatement = LBLs.get(LBL).intValue();
	                }
	            }
	        }

	        private final Expression condition;
	        private final String LBL;
	    }
	    
	    
	    public class VariableExpression implements Expression {
	        public VariableExpression(String name) {
	            this.name = name;
	        }
	        
	        public Value evaluate() {
	            if (vars.containsKey(name)) {
	                return vars.get(name);
	            }
	            return new NumVal(0);
	        }
	        
	        private final String name;
	    }
	    
	    
	    public class opExpression implements Expression {
	        public opExpression(Expression l, char op,
	                                  Expression r) {
	            this.l = l;
	            this.op = op;
	            this.r = r;
	        }
	        
	        public Value evaluate() {
	            Value lVal = l.evaluate();
	            Value rVal = r.evaluate();
	            
	            switch (op) {
	            case '=':
	                
	                if (lVal instanceof NumVal) {
	                    return new NumVal((lVal.toNumber() ==
	                                            rVal.toNumber()) ? 1 : 0);
	                } else {
	                    return new NumVal(lVal.toString().equals(
	                                           rVal.toString()) ? 1 : 0);
	                }
	            case '+':
	                
	                if (lVal instanceof NumVal) {
	                    return new NumVal(lVal.toNumber() +
	                                           rVal.toNumber());
	                } else {
	                    return new StringValue(lVal.toString() +
	                            rVal.toString());
	                }
	            case '-':
	                return new NumVal(lVal.toNumber() -
	                        rVal.toNumber());
	            case '*':
	                return new NumVal(lVal.toNumber() *
	                        rVal.toNumber());
	            case '/':
	                return new NumVal(lVal.toNumber() /
	                        rVal.toNumber());
	            case '<':
	                
	                if (lVal instanceof NumVal) {
	                    return new NumVal((lVal.toNumber() <
	                                            rVal.toNumber()) ? 1 : 0);
	                } else {
	                    return new NumVal((lVal.toString().compareTo(
	                                           rVal.toString()) < 0) ? 1 : 0);
	                }
	            case '>':
	                
	                if (lVal instanceof NumVal) {
	                    return new NumVal((lVal.toNumber() >
	                                            rVal.toNumber()) ? 1 : 0);
	                } else {
	                    return new NumVal((lVal.toString().compareTo(
	                            rVal.toString()) > 0) ? 1 : 0);
	                }
	            }
	            throw new Error("Unknown op.");
	        }
	        
	        private final Expression l;
	        private final char op;
	        private final Expression r;
	    }
	    
	    
	    public interface Value extends Expression {
	        
	        String toString();
	        
	        
	        double toNumber();
	    }
	    
	    
	    public class NumVal implements Value {
	        public NumVal(double value) {
	            this.value = value;
	        }
	        
	        @Override public String toString() { return Double.toString(value); }
	        public double toNumber() { return value; }
	        public Value evaluate() { return this; }

	        private final double value;
	    }
	    
	    
	    public class StringValue implements Value {
	        public StringValue(String value) {
	            this.value = value;
	        }
	        
	        @Override public String toString() { return value; }
	        public double toNumber() { return Double.parseDouble(value); }
	        public Value evaluate() { return this; }

	        private final String value;
	    }

	    
	    public TplMain() {
	        vars = new HashMap<String, Value>();
	        LBLs = new HashMap<String, Integer>();
	        
	        InputStreamReader converter = new InputStreamReader(System.in);
	        lineIn = new BufferedReader(converter);
	    }

	    
	    public void interpret(String src) {
	        
	        List<Token> tokens = lex(src);
	        
	        
	        Parser parser = new Parser(tokens);
	        List<Statement> statements = parser.parse(LBLs);
	        
	        
	        currentStatement = 0;
	        while (currentStatement < statements.size()) {
	            int thisStatement = currentStatement;
	            currentStatement++;
	            statements.get(thisStatement).execute();
	        }
	    }
	    
	    private final Map<String, Value> vars;
	    private final Map<String, Integer> LBLs;
	    
	    private final BufferedReader lineIn;
	    
	    private int currentStatement;
	    
	    
	    private static String readFile(String path) {
	        try {
	            FileInputStream stream = new FileInputStream(path);
	            
	            try {
	                InputStreamReader input = new InputStreamReader(stream,
	                    Charset.defaultCharset());
	                Reader reader = new BufferedReader(input);
	                
	                StringBuilder builder = new StringBuilder();
	                char[] buffer = new char[8192];
	                int read;
	                
	                while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
	                    builder.append(buffer, 0, read);
	                }
	                
	                
	                builder.append("\n");
	                
	                return builder.toString();
	            } finally {
	                stream.close();
	            }
	        } catch (IOException ex) {
	            return null;
	        }
	    }
	}

