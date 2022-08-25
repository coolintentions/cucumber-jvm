package io.cucumber.junit.platform.engine;

import io.cucumber.core.gherkin.Pickle;
import io.cucumber.plugin.event.Node;

import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum DefaultNamingStrategy implements NamingStrategy {

    LONG {
        @Override
        public String name(Node node) {

            String nameOrKeyword = nameOrKeyword(node);
            //Replace line 'feature name - scenario outline - pickle' to 'Examples'
            if (nameOrKeyword.contains("Example")) {
                return nameOrKeyword;
            } else {
                StringBuilder builder = new StringBuilder();
                builder.append(nameOrKeyword(node));
                node = node.getParent().orElse(null);
                while (node != null) {
                    builder.insert(0, " - ");
                    builder.insert(0, nameOrKeyword(node));
                    node = node.getParent().orElse(null);
                }
                nameOrKeyword = builder.toString();
            }

            return nameOrKeyword;
        }

        @Override
        public String name(Node node, Pickle pickle) {

            String nameOrKeyword = nameOrKeyword(node);
            // Pickle includes resolved scenario outline name.
            // Replace 'Example #x' with 'scenario outline' name from pickle.
            if (nameOrKeyword.contains("Example #")) {
                nameOrKeyword = pickle.getName();
            }

            StringBuilder builder = new StringBuilder();

            // Get second parent from node. Could be done with java streams but minimum JDK should be 1.9 to use stream on optional (.flatMap(Optional::stream))
            node = node.getParent().orElse(null); // get first parent
            node = node.getParent().orElse(null); // get second parent

            boolean printDash = false;
            while (node != null) {
                if (printDash) { // Remove last dash in string 'feature name - scenario outline -'
                    builder.insert(0, " - ");
                }
                printDash = true;
                builder.insert(0, getNameForNode(node, nameOrKeyword));
                node = node.getParent().orElse(null);
            }

            return builder.toString();
        }

        // Use regex to detect cucumber string interpolation placeholder like '<example>'.
        // If detected return pickle resolved name; else default name or keyword.
        private String getNameForNode(Node node, String name) {
            String nameOrKeyword = nameOrKeyword(node);
            Pattern pattern = Pattern.compile("<.*>");
            Matcher matcher = pattern.matcher(nameOrKeyword);
            return matcher.find() ? name : nameOrKeyword;
        }
    },

    SHORT {
        @Override
        public String name(Node node) {
            return nameOrKeyword(node);
        }

        @Override
        public String name(Node node, Pickle pickle) {
            String nameOrKeyword = nameOrKeyword(node);

            // Pickle includes resolved scenario outline name.
            // Replace 'Example #x' with 'scenario outline' name from pickle.
            if (nameOrKeyword.contains("Example #")) {
                nameOrKeyword = pickle.getName();
            }
            return nameOrKeyword;
        }
    };

    static DefaultNamingStrategy getStrategy(String s) {
        return valueOf(s.toUpperCase(Locale.ROOT));
    }

    private static String nameOrKeyword(Node node) {
        Supplier<String> keyword = () -> node.getKeyword().orElse("Unknown");
        return node.getName().orElseGet(keyword);
    }

}
