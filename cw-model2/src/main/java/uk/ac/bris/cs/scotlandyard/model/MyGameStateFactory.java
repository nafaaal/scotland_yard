package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import javax.annotation.Nonnull;

import com.google.common.collect.Lists;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

/*****
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	private final class MyGameState implements GameState {

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableList<Player> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;


		private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining, final ImmutableList<LogEntry> log, final Player mrX, final ImmutableList<Player> detectives) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
		}


		@Override public GameSetup getSetup() { return setup; }
		@Override public ImmutableSet<Piece> getPlayers() {

			//return ImmutableList.copyOf(Lists.asList(mrX, detectives));
		}
		@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			for (Player d : detectives) {
				if (d.equals(detective)) return Optional.of(d.location());
				}
			return Optional.empty();
		}
		@Override public Optional<TicketBoard> getPlayerTickets(Piece piece) { return Optional.empty(); }
		@Override public ImmutableList<LogEntry> getMrXTravelLog() { return log; }
		@Override public ImmutableSet<Piece> getWinner() { return null; }
		@Override public ImmutableSet<Move> getAvailableMoves() { return null; }
		@Override public GameState advance(Move move) { return null; }
	}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {

		if (mrX == null) throw new NullPointerException();
		if (detectives == null) throw new NullPointerException();
		if (!mrX.isMrX()) throw new IllegalArgumentException();

		Set<Integer> set1 = new HashSet<Integer>();
		for (Player r : detectives){
			if (r.isMrX()) throw new IllegalArgumentException();

			set1.add(r.location());

			if (r.has(Ticket.SECRET)) throw new IllegalArgumentException();

			if (r.has(Ticket.DOUBLE)) throw new IllegalArgumentException();
		}

		if(set1.size() < detectives.size()){
			throw new IllegalArgumentException();
		}


		Set<Player> set2 = new HashSet<Player>(detectives);
		if(set2.size() < detectives.size()){
			throw new IllegalArgumentException();
		}
		if (setup.rounds.isEmpty()) throw new IllegalArgumentException("Rounds is empty!");
		if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Rounds is empty!");
		//throw new RuntimeException("Implement me!");
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

}