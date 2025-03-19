import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Lexer {
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "\\b(Read|Display|print)\\b|[a-zA-Z_]\\w*|\\d+(\\.\\d+)?|[=+\\-*/();]"
    );

    public static List<String> tokenize(String code) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(code);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }   
        return tokens;
    }
}

class ASTNode {
    enum Type { ASSIGNMENT, READ, DISPLAY, EXPRESSION, PRINT }
    Type type;
    String variable;
    String expression;

    ASTNode(Type type, String variable, String expression) {
        this.type = type;
        this.variable = variable;
        this.expression = expression;
    }
}

class Parser {
    private List<String> tokens;
    private int index = 0;

    public Parser(List<String> tokens) {
        this.tokens = tokens;
    }

    public List<ASTNode> parse() {
        List<ASTNode> ast = new ArrayList<>();
        while (index < tokens.size()) {
            ast.add(parseStatement());
        }
        return ast;
    }

    private ASTNode parseStatement() {
        String token = tokens.get(index);

        switch (token) {
            case "Read":
                return parseReadStatement();
            case "Display":
            case "print":
                return parseDisplayStatement();
            default:
                return parseAssignmentStatement();
        }
    }

    private ASTNode parseReadStatement() {
        index++; // Skip "Read"
        String varName = tokens.get(index++);
        consumeSemicolon();
        return new ASTNode(ASTNode.Type.READ, varName, null);
    }

    private ASTNode parseDisplayStatement() {
        index++; // Skip "Display" or "print"
        String varName = tokens.get(index++);
        consumeSemicolon();
        return new ASTNode(ASTNode.Type.DISPLAY, varName, null);
    }

    private ASTNode parseAssignmentStatement() {
        String varName = tokens.get(index++);
        index++; // Skip "="
        String expression = parseExpression();
        return new ASTNode(ASTNode.Type.ASSIGNMENT, varName, expression);
    }

    private String parseExpression() {
        StringBuilder expression = new StringBuilder();
        while (index < tokens.size() && !tokens.get(index).equals(";")) {
            expression.append(tokens.get(index++)).append(" ");
        }
        consumeSemicolon();
        return expression.toString().trim();
    }

    private void consumeSemicolon() {
        if (index < tokens.size() && tokens.get(index).equals(";")) {
            index++;
        }
    }
}

class Interpreter {
    private Map<String, Double> variables = new HashMap<>();

    public void execute(List<ASTNode> ast) {
        for (ASTNode node : ast) {
            try {
                switch (node.type) {
                    case READ:
                        variables.put(node.variable, 0.0);
                        break;
                    case ASSIGNMENT:
                        variables.put(node.variable, evaluateExpression(node.expression));
                        break;
                    case DISPLAY:
                    case PRINT:
                        if (variables.containsKey(node.variable)) {
                            System.out.println(node.variable + " = " + variables.get(node.variable));
                        } else {
                            System.out.println("Error: Undefined variable " + node.variable);
                        }
                        break;
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private double evaluateExpression(String expression) {
        return new ExpressionEvaluator(expression, variables).evaluate();
    }
}

class ExpressionEvaluator {
    private String expression;
    private Map<String, Double> variables;

    public ExpressionEvaluator(String expression, Map<String, Double> variables) {
        this.expression = expression;
        this.variables = variables;
    }

    public double evaluate() {
        return parseExpression(expression.split("\\s+"));
    }

    private double parseExpression(String[] tokens) {
        Stack<Double> values = new Stack<>();
        Stack<String> operators = new Stack<>();

        for (String token : tokens) {
            if (isNumeric(token)) {
                values.push(Double.parseDouble(token));
            } else if (variables.containsKey(token)) {
                values.push(variables.get(token));
            } else if (isOperator(token)) {
                while (!operators.isEmpty() && hasPrecedence(token, operators.peek())) {
                    values.push(applyOperator(operators.pop(), values.pop(), values.pop()));
                }
                operators.push(token);
            }
        }

        while (!operators.isEmpty()) {
            values.push(applyOperator(operators.pop(), values.pop(), values.pop()));
        }

        return values.pop();
    }

    private boolean isNumeric(String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isOperator(String token) {
        return token.matches("[+\\-*/]");
    }

    private boolean hasPrecedence(String op1, String op2) {
        if ((op1.equals("*") || op1.equals("/")) && (op2.equals("+") || op2.equals("-"))) {
            return false;
        }
        return true;
    }

    private double applyOperator(String operator, double b, double a) {
        switch (operator) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/":
                if (b == 0) throw new ArithmeticException("Division by zero");
                return a / b;
            default: throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }
}



public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\nEnter an expression (or 'exit' to quit):");
            String expression = scanner.nextLine();
            
            if (expression.equalsIgnoreCase("exit")) {
                break;
            }
            
            try {
                List<String> tokens = Lexer.tokenize(expression);
                Parser parser = new Parser(tokens);
                List<ASTNode> ast = parser.parse();

                Interpreter interpreter = new Interpreter();
                interpreter.execute(ast);
            } catch (Exception e) {
                System.out.println("Error processing expression: " + e.getMessage());
            }
        }
        
        System.out.println("Interpreter closed.");
        scanner.close();
    }
}