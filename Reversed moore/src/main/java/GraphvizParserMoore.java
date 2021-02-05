

import com.google.common.collect.Lists;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.oracle.membership.SimulatorOracle;
import net.automatalib.automata.transducers.impl.compact.CompactMoore;

import net.automatalib.util.automata.builders.AutomatonBuilders;
import net.automatalib.util.automata.builders.MooreBuilder;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;


public class GraphvizParserMoore {




    public static class State {
        public final String state;
        public final String output;

        public State(String state, String output) {
            this.state = state;
            this.output = output;
        }
    }

    public static class Edge {
        public final String from;
        public final String to;
        public final String label;

        public Edge(String from, String to, String label) {
            this.from = from;
            this.to = to;
            this.label = label;
        }
    }

    public final List<GraphvizParserMoore.State> states;
    public final List<GraphvizParserMoore.Edge> edges;
    public GraphvizParserMoore.State initialState;


    GraphvizParserMoore(Path filename) throws IOException {
        states = new ArrayList<>();
        edges = new ArrayList<>();

        Scanner s = new Scanner(filename);
        while (s.hasNextLine()) {
            String line = s.nextLine();

            if (!line.contains("label")) continue;

            if (line.contains("->")) {
                int e1 = line.indexOf('-');
                int e2 = line.indexOf('[', e1);
                int b3 = line.indexOf('"', e2);
                int e3 = line.indexOf('"', b3 + 1);

                String from = line.substring(0, e1).trim();
                String to = line.substring(e1 + 2, e2).trim();
                String label = line.substring(b3 + 1, e3).trim();

                edges.add(new Edge(from, to, label));
            } else {
                int e1 = line.indexOf('[');
                int b2 = line.indexOf("label=\"",e1 + 1);
                int e2 = line.indexOf('"', b2 + 7);
                String state = line.substring(0, e1).trim();
                String output = line.substring(b2+7, e2).trim();

                State st = new State(state, output);
                if (initialState == null) initialState = st;

                states.add(st);
            }
        }
    }

    CompactMoore<String, String> createMachine() {
        Set<String> inputs = new HashSet<>();
        for (Edge e : edges) {
            inputs.add(e.label);
        }

        List<String> inputList = Lists.newArrayList(inputs.iterator());
        Alphabet<String> alphabet = Alphabets.fromCollection(inputList);

        assert (initialState != null);
        MooreBuilder<?, String, ?, String, CompactMoore<String, String>>.MooreBuilder__1 builder = AutomatonBuilders.<String, String>newMoore(alphabet).withInitial(initialState.state);

        for (State s : states) {
            builder.withOutput(s.state, s.output);
        }

        for (Edge e : edges) {
            builder.from(e.from).on(e.label).to(e.to);
        }

        return builder.create();
    }



}