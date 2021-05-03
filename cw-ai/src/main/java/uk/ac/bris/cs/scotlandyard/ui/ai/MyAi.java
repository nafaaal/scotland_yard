package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	private static final class MyGameState implements Board.GameState {
		private static GameSetup setup = null;
		private static ImmutableSet<Piece> remaining;
		private static ImmutableList<LogEntry> log;
		private static Player mrX;
		private static ImmutableList<Player> detectives;
		private static ImmutableList<Player> everyone;
		private static ImmutableSet<Move> moves;
		private static ImmutableSet<Piece> winner;
		private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining, final ImmutableList<LogEntry> log, final Player mrX, final ImmutableList<Player> detectives) {
			MyGameState.setup = setup;
			MyGameState.remaining = remaining;
			MyGameState.log = log;
			MyGameState.mrX = mrX;
			MyGameState.detectives = detectives;
			Set<Player> temp = new HashSet<>(detectives);
			temp.add(mrX);
			everyone = ImmutableList.copyOf(temp);
		}
		@Nonnull @Override public GameSetup getSetup() { return setup; }
		@Nonnull @Override public ImmutableSet<Piece> getPlayers() { return everyone.stream().map(Player::piece).collect(ImmutableSet.toImmutableSet()); }
		@Nonnull @Override public Optional<Integer> getDetectiveLocation(Piece.Detective detective) { return Optional.empty();}
		@Nonnull @Override public Optional<TicketBoard> getPlayerTickets(Piece piece) { return Optional.empty(); }
		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() { return log; }
		@Nonnull @Override public ImmutableSet<Piece> getWinner() { return winner; }
		@Nonnull @Override public ImmutableSet<Move> getAvailableMoves() { return moves; }
		private static void updateRemaining(Move move) {
			Piece lastActed = move.commencedBy();
			Set<Piece> remains = new HashSet<>(remaining);
			remains.remove(lastActed);
			if (remains.size() == 0) {
				//remaining is populated accordingly based on who played last
				if (lastActed.isMrX()) {
					remains = detectives.stream()
							.filter(MyGameState::hasTickets)
							.map(Player::piece)
							.collect(Collectors.toSet());
				}
				if (lastActed.isDetective()) {
					remains.add(mrX.piece());
				}
			}
			remaining = ImmutableSet.copyOf(remains);
		}
		private static boolean hasTickets(Player player) {
			return player.tickets()
					.entrySet()
					.stream()
					.anyMatch(ticket -> ticket.getValue() > 0);
		}
		private static void updateTickets(Move move) {
			Player player = pieceToPlayer(move.commencedBy());

			Function<Move.SingleMove, Player> smf = m -> player.use((m).ticket).at((m).destination);
			Function<Move.DoubleMove, Player> dmf = m -> {
				mrX = mrX.use(ScotlandYard.Ticket.DOUBLE).at(m.source());
				mrX = mrX.use((m).ticket1).at((m).destination1).use((m).ticket2).at((m).destination2);
				return mrX;
			};

			Move.Visitor<Player> visitor = new Move.FunctionalVisitor<>(smf, dmf);

			if (player.isMrX()) { // Have to check if mrX makes double move, and update accordingly.
				mrX = move.visit(visitor);
			}

			if (player.isDetective()) {// Detectives can only make a singleMove.
				Set<Player> updatedDet = new HashSet<>(detectives);
				updatedDet.remove(player);
				updatedDet.add(move.visit(visitor));
				mrX = mrX.give(move.tickets());
				detectives = ImmutableList.copyOf(updatedDet);
			}
		}
		private static Player pieceToPlayer(Piece piece) {
			return everyone.stream()
					.filter(player -> player.piece() == piece)
					.findFirst()
					.orElse(null);
		}
		private void updateLog(Move move) {
			Function<Move.SingleMove, ArrayList<LogEntry>> smf = m -> {
				if (setup.rounds.get(log.size())) {
					return new ArrayList<>(List.of(LogEntry.reveal(m.ticket, m.destination)));
				} else {
					return new ArrayList<>(List.of(LogEntry.hidden(m.ticket)));
				}
			};
			Function<Move.DoubleMove, ArrayList<LogEntry>> dmf = m -> {
				if (setup.rounds.get(log.size()) && setup.rounds.get(log.size() + 1)) {
					return new ArrayList<>(List.of(LogEntry.reveal(m.ticket1, m.destination1), LogEntry.reveal(m.ticket2, m.destination2)));
				} else if (setup.rounds.get(log.size() + 1)) {
					return new ArrayList<>(List.of(LogEntry.hidden(m.ticket1), LogEntry.reveal(m.ticket2, m.destination2)));
				} else if (setup.rounds.get(log.size())) {
					return new ArrayList<>(List.of(LogEntry.reveal(m.ticket1, m.destination2), LogEntry.hidden(m.ticket2)));
				} else {
					return new ArrayList<>(List.of(LogEntry.hidden(m.ticket1), LogEntry.hidden(m.ticket2)));
				}
			};

			Player player = pieceToPlayer(move.commencedBy());
			List<LogEntry> tempLog = new ArrayList<>(List.copyOf(log));

			if (player.isMrX()) {
				Move.Visitor<ArrayList<LogEntry>> visitor = new Move.FunctionalVisitor<>(smf, dmf);
				tempLog.addAll(move.visit(visitor));
			}
			log = ImmutableList.copyOf(tempLog);
		}
		@Nonnull @Override public GameState advance(Move move) {
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
			updateRemaining(move);
			updateLog(move);
			updateTickets(move);
			return new MyGameState(setup, remaining, log, mrX, detectives);
		}
	}

	@Nonnull @Override public String name() { return "Name me!"; }

	@Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
		ImmutableList<Move> moves = board.getAvailableMoves().asList();

		List<Piece> detectivePieces = board.getPlayers().stream().filter(Piece::isDetective).collect(Collectors.toList());
		Piece mrX = board.getPlayers().stream().filter(Piece::isDetective).findAny().orElse(null);

		ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph = board.getSetup().graph;
		// find out where the detective pieces are in the graph
		//log there next possible moves, and update state within somehow?

		// adjacent locations of from where mrx can move
		// now maybe check if mrX has the tickets to move there?
		List<Integer> locationss = new ArrayList<>();
		for (Move m : moves){
			locationss.addAll(graph.adjacentNodes(m.source()));
		}

		//need to check if detective there, and then avoid., no wait
		//need to get detective pieces and locations somehowplis
//		List<Move> mov = new ArrayList<>();
//		for (Move m : moves){
//			if (m instanceof Move.SingleMove){
//				if(!locationss.contains(((Move.SingleMove) m).destination)) mov.add(m);
//			}
//			if (m instanceof Move.DoubleMove){
//				if(!locationss.contains(((Move.DoubleMove) m).destination2)) mov.add(m);
//			}
//		}

//		List<Integer> locations = new ArrayList<>();
//		ImmutableSet<Piece> players = board.getPlayers();
		//ERRORS IDK WHY
//		for (Piece p : players){
//			locations.add(board.getDetectiveLocation((Piece.Detective) p).orElse(-1));
//		}

//		ImmutableList<LogEntry> log = board.getMrXTravelLog();
//		ImmutableSet<Piece> winner = board.getWinner();


		//currently moves shows moves of mrX
		//so mrX has to select a move which maximises the distance from him and current detective locations

		//locations where mrX can land
//		Set<Integer> endLocations = new HashSet<>();
//		// moves of mrX
//		for (Move m : moves){
//			if (m instanceof Move.SingleMove){
//				endLocations.add(((Move.SingleMove) m).destination);
//			} else {
//				endLocations.add(((Move.DoubleMove) m).destination2);
//			}
//		}

//		from list of locations maybe read the text file to find out the next locations?

//		Set<Integer> finalLocations = new HashSet<>();
////		finalLocation cannot be a location a detective can move to
//		for (Integer l : endLocations){
//			if (!locations.contains(l)) finalLocations.add(l);
//		}

		// returns a random move, replace with your own implementation

		return moves.get(new Random().nextInt(moves.size()));
		}
	}
