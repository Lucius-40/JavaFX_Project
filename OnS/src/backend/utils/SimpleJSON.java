package backend.utils;

import java.lang.reflect.Array;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.management.RuntimeErrorException;

// import SimpleJSON.ParseJSON.JsonNode;


public class SimpleJSON {

    public static JSONBuilder create(){
        return new JSONBuilder(); 
    }

    public static ParseJSON.JsonNode parse(String jsoString){
        return new ParseJSON(jsoString).parse();
    }

    public static ParseJSON.JsonNode parseFromBuilder(JSONBuilder builder){
        return new ParseJSON(builder.build()).parse();
    }

    public static class JSONBuilder{
        private final StringBuilder json;
        private final Stack<Context> contextStack;

        private  static class Context{
            boolean isFirst;
            boolean isArray;

            Context(boolean isFirst, boolean isArray){
                this.isFirst = isFirst;
                this.isArray = isArray;
            }
        }
//        private boolean isFirst = true;

        public JSONBuilder() {
            this.json = new StringBuilder();
            this.contextStack = new Stack<>();
            this.contextStack.push(new Context(true, false));
        }

        private void addComma(){
            Context current = contextStack.peek();
            if(!current.isFirst){
                json.append(",");
            }
            current.isFirst = false;
        }

        public JSONBuilder startObject(){
            addComma();
            json.append("{");
            contextStack.push(new Context(true, false));
            return this;
        }

        public JSONBuilder endObject(){
            json.append("}");
            contextStack.pop();
            return this;
        }

        public  JSONBuilder startArray(String key){
            addComma();
            json.append("\"").append(key).append("\":[");
            contextStack.push(new Context(true, true));
            return this;
        }

        public JSONBuilder endArray(){
            json.append("]");
            contextStack.pop();
            return this;
        }

        public JSONBuilder addString(String key, String value){
//            if(!isFirst) json.append(",");
            addComma();
            json.append("\"").append(key).append("\": \"").append(escapeString(value)).append("\"");
            return this;
        }

        public JSONBuilder addNumber(String key, double value){
            addComma();
            json.append("\"").append(key).append("\": ").append(value);
            return this;
        }

        public JSONBuilder addBoolean(String key, boolean value){
            addComma();
            json.append("\"").append(key).append("\": ").append(value);
            return this;
        }

        public JSONBuilder startObjectInArray(){
            addComma();
            json.append("{");
            contextStack.push(new Context(true, false));
            return this;
        }

        public JSONBuilder endObjectInArray(){
            json.append('}');
            contextStack.pop();
            return this;
        }

        private String escapeString(String str){
            if(str == null) return  "";
            return str.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }

        public String build(){
            return json.toString();
        }

        public String buildPretty(){
            return formatJSON(json.toString());
        }

        private String formatJSON(String json){
            StringBuilder pretty = new StringBuilder();
            int indent = 0;
            boolean inString = false;

            for(int i=0; i<json.length(); i++){
                char c = json.charAt(i);

                if(c == '"' && (i == 0 || json.charAt(i-1) != '\\')){
                    inString = !inString;
                }

                if(!inString){
                    switch (c){
                        case '{':
                        case '[':
                            pretty.append(c).append('\n');
                            indent++;
                            pretty.append("  ".repeat(indent));
                            break;
                        case '}':
                        case ']':
                            pretty.append('\n');
                            indent--;
                            pretty.append("  ".repeat(indent)).append(c);
                            break;
                        case ',':
                            pretty.append(c).append('\n');
                            pretty.append("  ".repeat(indent));
                            break;
                        default:
                            pretty.append(c);
                    }
                }else{
                    pretty.append(c);
                }
            }
            return pretty.toString();
        }

        public JSONBuilder addRaw(String key, String rawValue) {
            addComma();
            json.append("\"").append(key).append("\": ").append(rawValue);
            return this;
        }

        public JSONBuilder addNull(String key) {
            addComma();
            json.append("\"").append(key).append("\": null");
            return this;
        }

        public ParseJSON.JsonNode parseJSON(){
            return new ParseJSON(this.build()).parse();
        }
    }

    public static class ParseJSON{
        private String input;
        private int pos = 0;

        public ParseJSON(String input){
            this.input = input;
        }

        private void skipWhitespace(){
            while(this.pos < input.length() && Character.isWhitespace(this.peek())){
                this.pos++;
            }
        }

        private char peek(){
            return this.input.charAt(pos);
        }

        private boolean match(char expected){
            if(this.pos < input.length() && this.input.charAt(this.pos) == expected){
                this.pos++;
                return true;
            }
            return false;
        }

        private void expect(char c){
            if(!match(c)){
                throw new RuntimeException("Expecting " + c + " in this place");
            }
        }

        private void expect(String s){
            for(char c: s.toCharArray()){
                expect(c);
            }
        }

        public interface JsonNode {
            default boolean isObject() { return this instanceof JsonObject; }
            default boolean isArray() { return this instanceof JsonArray; }
            default boolean isString() { return this instanceof JsonString; }
            default boolean isNumber() { return this instanceof JsonInt; }
            default boolean isBoolean() { return this instanceof JsonBoolean; }
            default boolean isNull() { return this instanceof JsonNull; }
            
            default JsonObject asObject() { return (JsonObject) this; }
            default JsonArray asArray() { return (JsonArray) this; }
            default String asString() { return ((JsonString) this).str; }
            default int asInt() { return (int) ((JsonInt) this).member; }
            default boolean asBoolean() { return ((JsonBoolean) this).bool; }
            
            default JsonNode get(String path) {
                String[] parts = path.split("\\.");
                JsonNode current = this;
                
                for (String part : parts) {
                    if (current.isObject()) {
                        current = current.asObject().members.get(part);
                    } else if (current.isArray() && part.matches("\\d+")) {
                        current = current.asArray().members.get(Integer.parseInt(part));
                    } else {
                        return null;
                    }
                }
                return current;
            }

            default String getString(String path){
                JsonNode node = get(path);
                return node!=null && node.isString()?node.asString():null;
            }

            default Integer getInt(String path){
                JsonNode node = get(path);
                return node!=null && node.isNumber()?node.asInt():null;
            }

            default Boolean getBoolean(String path){
                JsonNode node = get(path);
                return node!=null && node.isBoolean()?node.asBoolean():null;
            }
        };

        public static class JsonObject implements JsonNode{
            public final Map<String, JsonNode> members = new HashMap<>();
            @Override public String toString(){
                return members.toString();
            }
        }

        public static class JsonArray implements JsonNode{
            public final List<JsonNode> members = new ArrayList<>();
            @Override public String toString(){
                return members.toString();
            }
        }

        public static class JsonInt implements JsonNode{
            public final double member;
            public JsonInt(double value){
                this.member = value;
            }

            @Override public String toString(){
                return Double.toString(this.member);
            }
        }

        public static class JsonString implements JsonNode{
            public final String str;
            public JsonString(String str){
                this.str = str;
            }
            @Override public String toString(){
                return '\"' + str + '\"';
            }
        }

        public static class JsonBoolean implements JsonNode{
            public final boolean bool;
            public JsonBoolean(boolean bool){
                this.bool = bool;
            }

            @Override public String toString(){
                return Boolean.toString(this.bool);
            }
        }

        public static class JsonNull implements JsonNode {
            @Override public String toString() { return "null"; }
        }

        private double parseNumber(){
            int start = this.pos;
            if(this.peek() == '-') this.pos++;
            while(this.pos < this.input.length() && Character.isDigit(this.peek())){
                this.pos++;
            }
            if(this.match('.')){
                this.pos++;
                while(this.pos < this.input.length() && Character.isDigit(this.peek())){
                    this.pos++;
                }
            }

            return Double.parseDouble(this.input.substring(start, this.pos));
        }

        private String parseString(){
            expect('"');
            StringBuilder string = new StringBuilder();
            while(this.pos < this.input.length()){
                char a = input.charAt(pos++);
                if(a == '"') break;
                if(a == '\\'){
                    char c = input.charAt(pos++);

                    switch (c) {
                        case '\\': string.append('\\'); break;
                        case '"': string.append('"'); break;
                        case '/': string.append('/'); break;
                        case 'b': string.append('\b'); break;
                        case 'f': string.append('\f'); break;
                        case 'r': string.append('\r'); break;
                        case 't': string.append('\t'); break;
                        case 'n': string.append('\n'); break;
                        case 'u': 
                            String hex = input.substring(pos, pos+4);
                            string.append((char)Integer.parseInt(hex, 16));
                            pos += 4;
                            break;
                    
                        default:
                            throw new RuntimeException("Invalid escape \\" + c);
                    }
                }else{
                    string.append(a);
                }
            }

            return string.toString();
        }
        
        private JsonArray parseArray(){
            JsonArray array = new JsonArray();
            skipWhitespace();
            if(!match(']')){
                do {
                    skipWhitespace();
                    array.members.add(parseValue());
                    skipWhitespace();
                } while (match(','));
                expect(']');
            }
            return array;
        }

        private JsonObject parseObject(){
            JsonObject obj = new JsonObject();
            skipWhitespace();
            if(!match('}')){
                do {
                    skipWhitespace();
                    String key = parseString();
                    skipWhitespace();
                    expect(':');
                    JsonNode val = parseValue();
                    skipWhitespace();
                    obj.members.put(key, val);
                    skipWhitespace();
                } while (this.match(','));
                skipWhitespace();
                expect('}');
            }

            return obj;
        }

        private JsonNode parseValue(){
            skipWhitespace();
            if(this.match('{')) return parseObject();
            if(this.match('[')) return parseArray();
            if(this.peek() == '"') return new  JsonString(parseString());
            if(this.peek() == 't'){
                expect("true");
                return new JsonBoolean(true);
            }
            if(this.peek() == 'f'){
                expect("false");
                return new JsonBoolean(false);
            }
            if(this.peek() == 'n'){
                expect("null");
                return new JsonNull();
            }
            if(this.peek() == '-' || Character.isDigit(peek())){
                return new JsonInt(parseNumber());
            }

            throw new RuntimeException("Unexpected character "+ peek() + " found");
        }

        public JsonNode parse(){
            skipWhitespace();
            JsonNode result = parseValue();
            skipWhitespace();

            if(this.pos < this.input.length()){
                throw new RuntimeException("Unexpected trailing characters at the positon: " + this.pos);
            }

            return result;
        }
    }

    public static class Parser{
        JSONBuilder json;
        ParseJSON.JsonNode parsed;
        public 
        Parser(JSONBuilder json){
            this.json = json;
            parsed = new ParseJSON(json.build()).parse();
        }

        public ParseJSON.JsonNode getJSON(){
            return parsed;
        }
    }

    // Updated compile-java task to include utils directory
    // "command": "javac -d bin -cp bin --module-path 'C:/Program Files/javafx-sdk-21.0.7/lib' --add-modules javafx.controls,javafx.fxml src/HelloApplication.java src/backend/models/*.java src/backend/utils/*.java src/frontend/controllers/*.java"
}
