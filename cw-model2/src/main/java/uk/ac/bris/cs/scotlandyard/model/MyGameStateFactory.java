package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import javax.annotation.Nonnull;

import com.google.common.collect.Lists;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

/*****love you 
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

			List<Player> players = new ArrayList<>();
			players.add(mrX);
			players.addAll(detectives);
			everyone = ImmutableList.copyOf(players);

			moves = getAvailableMoves();
			winner = getWinner();


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



		}

		public class myBoard implements Board.TicketBoard{
			public ImmutableMap<Ticket, Integer> tickets;

			public myBoard(ImmutableMap<Ticket, Integer> tickets){
				this.tickets = tickets;
			}
			// NEED TO IMPLEMENT GET COUNT
			public int getCount(@Nonnull Ticket ticket){
				return 2;
			}

		}



		@Override public GameSetup getSetup() {
			return setup;
		}

		@Override public ImmutableSet<Piece> getPlayers() {
			List<Piece> playerPieces = new ArrayList<>();
			for(Player p : everyone){
				playerPieces.add(p.piece());
			}
			return ImmutableSet.copyOf(playerPieces);
		}

		@Override public Optional<Integer> getDetectiveLocation(Detective detective) {
			for (Player d : detectives) {
				if (d.piece().equals(detective)) return Optional.of(d.location());
				}
			return Optional.empty();
		}

		@Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			for (Player d : everyone) {
				if (d.piece().equals(piece)) {
					TicketBoard tickectt = new myBoard(d.tickets());
					return Optional.of(tickectt);
				}
			}
			return Optional.empty();
		}

		@Override public ImmutableList<LogEntry> getMrXTravelLog() { return log; }
		// NEED TO FIND WINNERS
		@Override public ImmutableSet<Piece> getWinner(){
			ImmutableSet<Piece> win =  ImmutableSet.of();;
			if (log.size() == 0) win = ImmutableSet.of();
			return win;
		}
		@Override public ImmutableSet<Move> getAvailableMoves() {
			Set<Move> temporary = new HashSet<>();
			for(Player p : everyone){
				temporary.addAll(makeDoubleMoves(setup, detectives, p, p.location()));
			}
			return ImmutableSet.copyOf(temporary);
		}
		@Override public GameState advance(Move move) { return null; }
	}

	private static ImmutableSet<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
		final var singleMoves = new ArrayList<SingleMove>();

		List<Integer> detectiveLocations = new ArrayList<>();
		for (Player p : detectives){
			detectiveLocations.add(p.location());
		}
		for(int destination : setup.graph.adjacentNodes(source)) {
			if (!(detectiveLocations.contains(destination))){
				for(Transport t : setup.graph.edgeValueOrDefault(source,destination,ImmutableSet.of())) {
					if (player.has(t.requiredTicket())) {
						singleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
					}
					if (player.has(Ticket.SECRET)) {
						singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
					}
				}
			}
		}
		return ImmutableSet.copyOf(singleMoves);
	}

	private static ImmutableSet<Move> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
		List<Move> singleMoves = new ArrayList<>(makeSingleMoves(setup, detectives, player, source));
		List<Move> allMoves = new ArrayList<>();
		ImmutableSet<SingleMove> temp;
		if (player.isMrX()){
			for (Move m : singleMoves){
				temp = makeSingleMoves(setup,detectives,player,((SingleMove)m).destination);
				for (Move n : temp){
					allMoves.add(new DoubleMove(player.piece(), m.source(), ((SingleMove)m).ticket, ((SingleMove)m).destination, ((SingleMove)n).ticket, ((SingleMove)n).destination));
				}
			}
		}
		return ImmutableSet.copyOf(allMoves);
	}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

}
