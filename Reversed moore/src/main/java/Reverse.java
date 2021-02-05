import de.learnlib.algorithms.lstar.AbstractExtensibleAutomatonLStar;
import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandler;
import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithms.lstar.closing.ClosingStrategies;
import de.learnlib.algorithms.lstar.closing.ClosingStrategy;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.api.query.Query;
import de.learnlib.datastructure.observationtable.ObservationTable;
import de.learnlib.datastructure.observationtable.Row;
import de.learnlib.filter.statistic.oracle.CounterOracle;
import de.learnlib.oracle.equivalence.AbstractTestWordEQOracle;
import de.learnlib.oracle.equivalence.RandomWordsEQOracle;
import de.learnlib.oracle.membership.SimulatorOracle;
import de.learnlib.util.MQUtil;
import net.automatalib.SupportsGrowingAlphabet;
import net.automatalib.automata.MutableDeterministic;
import net.automatalib.automata.concepts.MutableStateOutput;
import net.automatalib.automata.concepts.SuffixOutput;
import net.automatalib.automata.transducers.impl.compact.CompactMoore;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Reverse {

    private Reverse() {
    }

    public static class ReverseWrapper implements MembershipOracle<String, String> {
        MembershipOracle<String, String> delegate;

        @Override
        public void processQueries(Collection<? extends Query<String,String>> queries){

            for(Query q : queries) {
                q.getInput();
                Word<String> reversedInput = new WordBuilder<String>().append(q.getInput()).reverse().toWord();
                delegate.answerQuery(reversedInput);
                q.answer(delegate.answerQuery(reversedInput));
            }
        }
        public ReverseWrapper(MembershipOracle<String,String> rw) { delegate = rw;
        }
    }


    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();


        GraphvizParserMoore p = new GraphvizParserMoore(Paths.get("C:\\Users\\LG\\Documents\\MooreMachinesRS\\grid3x4.dot"));
        CompactMoore<String, String> target0 = p.createMachine();
        CompactMooreLS<String, String> target = new CompactMooreLS<>(target0);
        Alphabet<String> inputs = target0.getInputAlphabet();

        // MQs
        MembershipOracle<String, String> mqOracle = new ReverseWrapper(new SimulatorOracle<>(target));
        CounterOracle<String, String> countingOracle = new CounterOracle<>(mqOracle, "MQs");
        // EQ can be implemented with testing:

        AbstractTestWordEQOracle<MooreMachineLS<?, String, String>, String, String> eqOracle = new RandomWordsEQOracle<>(mqOracle, 0, 100, 100_000);

        // construct Learner instance
        LearningAlgorithm<MooreMachineLS<?, String, String>, String, String> learner = new ExtensibleLStarMooreLastSymbol<>(inputs, countingOracle);

        learner.startLearning();
        MooreMachineLS<?, String, String> hyp;
        int cexes = 0;
        while (true) {
            hyp = learner.getHypothesisModel();
            System.out.println("Number of states = " + hyp.size());

            DefaultQuery<String, String> ce = eqOracle.findCounterExample(hyp, inputs);
            if (ce == null) break;

            System.out.println("Counterexample " + cexes + ": " + ce.getInput().toString());
            cexes++;

            boolean refined = learner.refineHypothesis(ce);
            assert refined;
        }
        long end = System.currentTimeMillis();
        long total = end - start ;


        // report results
        System.out.println("-------------------------------------------------------");
        System.out.println("Number of counterexamples = " + cexes);
        System.out.println("Number of states = " + hyp.size());
        System.out.println(countingOracle.getStatisticalData());

System.out.println(total);












    }

    public static class ExtensibleLStarMooreLastSymbol<I, O> extends AbstractExtensibleAutomatonLStar<MooreMachineLS<?, I, O>, I, O, Integer, Integer, O, Void, CompactMooreLS<I, O>> {
        public ExtensibleLStarMooreLastSymbol(Alphabet<I> alphabet, MembershipOracle<I, O> oracle) {
            this(alphabet, oracle, new CompactMooreLS<>(alphabet), Collections.singletonList(Word.epsilon()), Collections.singletonList(Word.epsilon()), ObservationTableCEXHandlers.RIVEST_SCHAPIRE, ClosingStrategies.CLOSE_FIRST);
        }

        public ExtensibleLStarMooreLastSymbol(Alphabet<I> alphabet, MembershipOracle<I, O> oracle, CompactMooreLS<I, O> internalHyp, List<Word<I>> initialPrefixes, List<Word<I>> initialSuffixes, ObservationTableCEXHandler<? super I, ? super O> cexHandler, ClosingStrategy<? super I, ? super O> closingStrategy) {
            super(alphabet, oracle, internalHyp, initialPrefixes, initialSuffixes, cexHandler, closingStrategy);
        }

        @Override
        protected MooreMachineLS<?, I, O> exposeInternalHypothesis() {
            return internalHyp;
        }

        @Override
        protected O stateProperty(ObservationTable<I, O> table, Row<I> stateRow) {
            return table.cellContents(stateRow, 0);
        }

        @Override
        protected Void transitionProperty(ObservationTable<I, O> table, Row<I> stateRow, int inputIdx) {
            return null;
        }

        @Override
        protected SuffixOutput<I, O> hypothesisOutput() {
            return internalHyp;
        }
    }

    // LS stands for Last Symbol. In LearnLib, Moore machines are defined to return
    // a word of output, instead of just an output. This is annoying for us (because
    // these words cannot be reversed easily). So I will redefine the notion of a
    // Compact Moore machine, which then just outputs a single output. This uses
    // a CompactMoore internally, and delegates all calls. I don't know a better way
    // of doing this... But it works...
    public static class CompactMooreLS<I, O> implements MooreMachineLS<Integer, I, O>,
            MutableDeterministic<Integer, I, Integer, O, Void>,
            MutableStateOutput<Integer, O>,
            MutableDeterministic.StateIntAbstraction<I, Integer, O, Void>,
            MutableDeterministic.FullIntAbstraction<Integer, O, Void>,
            Serializable, SupportsGrowingAlphabet<I> {
        private final CompactMoore<I, O> delegate;

        public CompactMooreLS(CompactMoore<I, O> delegate) {
            this.delegate = delegate;
        }

        public CompactMooreLS(Alphabet<I> alphabet) {
            this(new CompactMoore<>(alphabet));
        }

        @Override
        public void setStateOutput(Integer state, O output) {
            delegate.setStateOutput(state, output);
        }

        @Override
        public O getStateOutput(Integer state) {
            return delegate.getStateOutput(state);
        }

        @Nullable
        @Override
        public Integer getTransition(int state, int input) {
            return delegate.getTransition(state, input);
        }

        @Nullable
        @Override
        public Integer getTransition(int state, I input) {
            return delegate.getTransition(state, input);
        }

        @Override
        public int getIntSuccessor(Integer transition) {
            return delegate.getIntSuccessor(transition);
        }

        @Override
        public void setTransition(Integer state, I input, @Nullable Integer transition) {
            delegate.setTransition(state, input, transition);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Integer addState(@Nullable O property) {
            return delegate.addState(property);
        }

        @Override
        public void setStateProperty(Integer state, O property) {
            delegate.setStateProperty(state, property);
        }

        @Override
        public void setTransitionProperty(Integer transition, Void property) {
            delegate.setTransitionProperty(transition, property);
        }

        @Override
        public void removeAllTransitions(Integer state) {
            delegate.removeAllTransitions(state);
        }

        @Override
        public Integer createTransition(Integer successor, Void properties) {
            return delegate.createTransition(successor, properties);
        }

        @Override
        public void setTransition(int state, int input, @Nullable Integer transition) {
            delegate.setTransition(state, input, transition);
        }

        @Override
        public void setTransition(int state, int input, int successor, Void property) {
            delegate.setTransition(state, input, successor, property);
        }

        @Override
        public void setTransition(int state, I input, @Nullable Integer transition) {
            delegate.setTransition(state, input, transition);
        }

        @Override
        public void setTransition(int state, I input, int successor, Void property) {
            delegate.setTransition(state, input, successor, property);
        }

        @Override
        public void setStateProperty(int state, O property) {
            delegate.setStateProperty(state, property);
        }

        @Override
        public Integer createTransition(int successor, Void property) {
            return delegate.createTransition(successor, property);
        }

        @Override
        public int addIntState(@Nullable O property) {
            return delegate.addIntState(property);
        }

        @Override
        public int addIntInitialState(@Nullable O property) {
            return delegate.addIntInitialState(property);
        }

        @Override
        public O getStateProperty(int state) {
            return delegate.getStateProperty(state);
        }

        @Override
        public int numInputs() {
            return delegate.numInputs();
        }

        @Override
        public int getIntInitialState() {
            return delegate.getIntInitialState();
        }

        @Override
        public Collection<Integer> getStates() {
            return delegate.getStates();
        }

        @Nullable
        @Override
        public Integer getTransition(Integer state, I input) {
            return delegate.getTransition(state, input);
        }

        @Override
        public Void getTransitionProperty(Integer transition) {
            return delegate.getTransitionProperty(transition);
        }

        @Override
        public Integer getSuccessor(Integer transition) {
            return delegate.getSuccessor(transition);
        }

        @Nullable
        @Override
        public Integer getInitialState() {
            return delegate.getInitialState();
        }

        @Override
        public void setInitialState(@Nullable Integer state) {
            delegate.setInitialState(state);
        }

        @Override
        public void setInitialState(int state) {
            delegate.setInitialState(state);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public void addAlphabetSymbol(I symbol) {
            delegate.addAlphabetSymbol(symbol);
        }
    }
}