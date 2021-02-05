
import net.automatalib.automata.UniversalDeterministicAutomaton;
import net.automatalib.automata.concepts.DetSuffixOutputAutomaton;
import net.automatalib.automata.graphs.TransitionEdge;
import net.automatalib.automata.graphs.UniversalAutomatonGraphView;
import net.automatalib.automata.visualization.AutomatonVisualizationHelper;
import net.automatalib.graphs.UniversalGraph;
import net.automatalib.ts.output.DeterministicStateOutputTS;
import net.automatalib.ts.output.MooreTransitionSystem;
import net.automatalib.visualization.VisualizationHelper;
import net.automatalib.words.WordBuilder;

import java.util.Collection;
import java.util.Map;

// LS stands for Last Symbol. In LearnLib, Moore machines are defined to return
// a word of output, instead of just an output. This is annoying for us (because
// these words cannot be reversed easily). So I will redefine the notion of a
// Moore machine, which then just outputs a single output.
public interface MooreMachineLS<S, I, O> extends UniversalDeterministicAutomaton<S, I, S, O, Void>,
        DetSuffixOutputAutomaton<S, I, S, O>,
        DeterministicStateOutputTS<S, I, S, O>,
        MooreTransitionSystem<S, I, S, O> {
    @Override
    default O computeStateOutput(S state, Iterable<? extends I> input) {
        // Probably not the most efficient way. But it works.
        WordBuilder<O> result = new WordBuilder<>();
        trace(state, input, result);
        return result.toWord().lastSymbol();
    }

    @Override
    default UniversalGraph<S, TransitionEdge<I, S>, O, TransitionEdge.Property<I, Void>> transitionGraphView(Collection<? extends I> inputs) {
        return new MooreMachineLS.MooreGraphView<>(this, inputs);
    }

    class MooreGraphView<S, I, O, A extends MooreMachineLS<S, I, O>>
            extends UniversalAutomatonGraphView<S, I, S, O, Void, A> {

        public MooreGraphView(A automaton, Collection<? extends I> inputs) {
            super(automaton, inputs);
        }

        @Override
        public VisualizationHelper<S, TransitionEdge<I, S>> getVisualizationHelper() {
            return new MooreLSVisualizationHelper<>(automaton);
        }
    }

    class MooreLSVisualizationHelper<S, I, O>
            extends AutomatonVisualizationHelper<S, I, S, MooreMachineLS<S, I, O>> {

        public MooreLSVisualizationHelper(MooreMachineLS<S, I, O> automaton) {
            super(automaton);
        }

        @Override
        public boolean getNodeProperties(S node, Map<String, String> properties) {
            if (!super.getNodeProperties(node, properties)) {
                return false;
            }

            O output = automaton.getStateOutput(node);
            properties.put(NodeAttrs.LABEL, String.valueOf(output));
            return true;
        }

    }
}
