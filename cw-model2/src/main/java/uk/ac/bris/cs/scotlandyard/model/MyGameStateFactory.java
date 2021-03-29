package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

public final class MyGameStateFactory implements Factory<GameState> {


	private final class MyGameState implements GameState {

		private final GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private final ImmutableList<Player> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;


		private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining, final ImmutableList<LogEntry> log, final Player mrX, final ImmutableList<Player> detectives) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

			List<Player> allPlayers = new ArrayList<>();
			allPlayers.add(mrX);
			allPlayers.addAll(detectives);
			everyone = ImmutableList.copyOf(allPlayers);

			moves = getAvailableMoves();
			winner = getWinner();

			// mrX cannot be null and detectives list cannot be empty.
			if ((mrX == null) || (detectives.size() == 0)) throw new NullPointerException();
			// check if Player mrX is actually MrX.
			if (mrX.isDetective()) throw new IllegalArgumentException();

			for (Player detective : detectives){
				// Ensure that all Players in detectives are actual detectives
				if (detective.isMrX()) throw new IllegalArgumentException();
				// Ensure that detectives do not have SECRET or DOUBLE tickets
				if (detective.has(Ticket.SECRET) || (detective.has(Ticket.DOUBLE))) throw new IllegalArgumentException();
			}

			Set<Integer> uniqueLocations = new HashSet<>();
			for (Player player : everyone){
				// Any duplicate locations would not be added to this set
				uniqueLocations.add(player.location());
			}
			//if duplicates -> uniqueLocations would be less than everyone and thus thrown an error
			if (uniqueLocations.size() < everyone.size()) throw new IllegalArgumentException();
			//Should have rounds to be a valid game.
			if (setup.rounds.isEmpty()) throw new IllegalArgumentException();
			//Check if graphs are not empty
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException();

		}

		 class myBoard implements TicketBoard{
			 ImmutableMap<Ticket, Integer> tickets;
			 myBoard(ImmutableMap<Ticket, Integer> tickets){
				this.tickets = tickets;
			}

			public int getCount(@Nonnull Ticket ticket){
				return tickets.getOrDefault(ticket,0);
			}

		}

		@Nonnull @Override public GameSetup getSetup() {
			return setup;
		}

		@Nonnull @Override public ImmutableSet<Piece> getPlayers() {
			List<Piece> playerPieces = new ArrayList<>();
			for(Player player : everyone){
				playerPieces.add(player.piece());
			}
			return ImmutableSet.copyOf(playerPieces);
		}

		@Nonnull @Override public Optional<Integer> getDetectiveLocation(Detective det) {
			for (Player detective : detectives) {
				if (detective.piece().equals(det)) return Optional.of(detective.location());
			}
			return Optional.empty();
		}

		@Nonnull @Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			for (Player player : everyone) {
				if (player.piece().equals(piece)) return Optional.of(new myBoard(player.tickets()));
			}
			return Optional.empty();
		}

		//NEED TO MAKE AND BUILD LOG?
		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		// NEED TO FIND WINNERS
		@Nonnull @Override public ImmutableSet<Piece> getWinner(){
			ImmutableSet<Piece> win =  ImmutableSet.of();;
			if (log.size() == 0) return win;
			return win;
		}

		@Nonnull @Override public ImmutableSet<Move> getAvailableMoves() {
			Set<Move> allMoves = new HashSet<>();
			for(Player player : everyone){
				allMoves.addAll(getAllMoves(setup, detectives, player, player.location()));
			}
			return ImmutableSet.copyOf(allMoves);
		}

		@Nonnull @Override public GameState advance(Move move) {
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			return null;
		}
	}

	private static ImmutableSet<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
		final var singleMoves = new ArrayList<SingleMove>();

		List<Integer> detectiveLocations = new ArrayList<>();
		for (Player detective : detectives){
			detectiveLocations.add(detective.location());
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

	private static ImmutableSet<Move> getAllMoves(GameSetup setup, List<Player> detectives, Player player, int source){
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
