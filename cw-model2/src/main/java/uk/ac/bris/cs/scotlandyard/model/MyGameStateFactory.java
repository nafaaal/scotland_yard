package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class MyGameStateFactory implements Factory<GameState> {


	private static final class MyGameState implements GameState {

		private final GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private ImmutableList<Player> detectives;
		private final ImmutableList<Player> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining, final ImmutableList<LogEntry> log, final Player mrX, final ImmutableList<Player> detectives) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.everyone = setEveryone();
			setMoves();
			findWinner();
			validate();
		}

		@Nonnull @Override public GameSetup getSetup() {
			return setup;
		}

		@Nonnull @Override public ImmutableSet<Piece> getPlayers() {
			return everyone.stream()
					.map(Player::piece)
					.collect(ImmutableSet.toImmutableSet());
		}

		@Nonnull @Override public Optional<Integer> getDetectiveLocation(Detective det) {
			Player player = detectives.stream()
					.filter(d -> d.equals(pieceToPlayer(det)))
					.findFirst()
					.orElse(null);
			return (player == null) ?  Optional.empty() : Optional.of(player.location());
		}

		@Nonnull @Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			Player player = everyone.stream()
					.filter(d -> d.equals(pieceToPlayer(piece)))
					.findFirst()
					.orElse(null);
			return (player == null) ?  Optional.empty() : Optional.of(tic -> player.tickets().getOrDefault(tic, 0));
		}

		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull @Override public ImmutableSet<Piece> getWinner() {
			return winner;
		}

		@Nonnull @Override public ImmutableSet<Move> getAvailableMoves() {
			return moves;
		}

		private ImmutableList<Player> setEveryone(){
			List<Player> allPlayers = new ArrayList<>();
			allPlayers.add(mrX);
			allPlayers.addAll(detectives);
			return ImmutableList.copyOf(allPlayers);
		}

		private void validate(){
			// detectives list cannot be empty.
			if ((detectives.size() == 0)) throw new NullPointerException();
			//  mrX cannot be null and check if Player mrX is actually MrX.
			if ((mrX == null) || mrX.isDetective()) throw new IllegalArgumentException();

			Set<Integer> detLocations = new HashSet<>();
			for (Player det : detectives){
				// Ensure that all Players in detectives are actual detectives
				if (det.isMrX()) throw new IllegalArgumentException();
				// Ensure that detectives do not have SECRET or DOUBLE tickets
				if (det.has(Ticket.SECRET) || (det.has(Ticket.DOUBLE))) throw new IllegalArgumentException();
				// Any duplicate detective locations would not be added to this set
				detLocations.add(det.location());
			}

			//if duplicates -> uniqueLocations would be less than detectives and thus thrown an error
			if ((detLocations.size() != detectives.size())) throw new IllegalArgumentException();
			//Should have rounds to be a valid game.
			if (setup.rounds.isEmpty()) throw new IllegalArgumentException();
			//Check if graphs are not empty
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException();
		}

		private void findWinner(){
			//mrX wins if all rounds end without detectives winning or detectives tickets run out
			if(gameRoundsReached() || detEmptyTickets()) {
				winner = ImmutableSet.of(mrX.piece());
				moves = ImmutableSet.of();
			//detectives mrX is stuck or caught
			} else if (isMrxCaught() || isMrxStuck()) {
				winner = ImmutableSet.copyOf(detectivePieces());
				moves = ImmutableSet.of();
			//otherwise game is not over -> no winner yet
			} else {
				winner = ImmutableSet.of();
			}
		}

		private boolean hasTickets(Player detective){
			return detective.tickets()
					.entrySet()
					.stream()
					.anyMatch(ticket -> ticket.getValue() > 0);
		}

		private boolean gameRoundsReached(){
			return (log.size() == setup.rounds.size()) && this.remaining.contains(mrX.piece());
		}

		private boolean detEmptyTickets(){
			return detectives.stream().noneMatch(this::hasTickets);
		}

		private boolean isMrxStuck(){
			return (remaining.contains(mrX.piece()) && getmrXMoves().size() == 0);
		}

		private boolean isMrxCaught(){
			return detectives.stream().anyMatch(det -> det.location() == mrX.location());
		}

		private ImmutableSet<Piece> detectivePieces() {
			return detectives.stream()
					.map(Player::piece)
					.collect(ImmutableSet.toImmutableSet());
		}

		private void setMoves() {
			Set<Move> allMoves = new HashSet<>();
			allMoves.addAll(getmrXMoves());
			allMoves.addAll(getDetMoves());
			moves = ImmutableSet.copyOf(allMoves);
		}

		private ImmutableSet<Move> getmrXMoves() {
			Player mrx = remaining.stream()
					.findFirst()
					.map(this::pieceToPlayer)
					.orElseThrow(IllegalArgumentException::new);
			return ImmutableSet.copyOf(getAllMoves(setup, detectives, mrx, mrx.location()));
		}

		private ImmutableSet<Move> getDetMoves() {
			Set<Move> detMoves = new HashSet<>();
			 remaining.stream()
					.map(this::pieceToPlayer)
					.forEach(detective -> detMoves.addAll(getAllMoves(setup, detectives, detective, detective.location())));
			return  ImmutableSet.copyOf(detMoves);
		}

		private ImmutableSet<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			Set<SingleMove> singleMoves = new HashSet<>();
			Set<Integer> detLocations = detectives.stream().map(Player::location).collect(Collectors.toSet());

			//Loops through each neighbouring node
			for(int destination : setup.graph.adjacentNodes(source)) {
				//Continue only if the node is not occupied
				if (!(detLocations.contains(destination))){
					//Loop through each edge and add the move if the player has the needed ticket
					for(Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
						if (player.has(t.requiredTicket())) {
							singleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
						}
					}
					// Secret ticket can move anywhere, therefore check for requiredTicket is not needed.
					if (player.has(Ticket.SECRET)) {
						singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
					}
				}
			}
			return ImmutableSet.copyOf(singleMoves);
		}

		private ImmutableSet<Move> getAllMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			//Get all singlemoves.
			Set<SingleMove> firstMoves = new HashSet<>(makeSingleMoves(setup, detectives, player, source));
			Set<DoubleMove> secondMoves = new HashSet<>();
			//Doublemove can be made if he has a Double ticket and if there are enough rounds.
			if (player.has(Ticket.DOUBLE) && (setup.rounds.size()-1 != log.size())){
				//Loop through each singlemove, and get the next move that can be made
				for (SingleMove first : firstMoves){
					// update tickets so that correct double moves can be found
					player = player.use(first.tickets());
					for (SingleMove second : makeSingleMoves(setup, detectives, player, first.destination)){
						secondMoves.add(new DoubleMove(player.piece(), first.source(), first.ticket, first.destination, second.ticket, second.destination));}
					// change tickets back
					player = player.give(first.tickets());
				}
			}
			Set<Move> allMoves = new HashSet<>();
			allMoves.addAll(firstMoves);
			allMoves.addAll(secondMoves); // note : empty if player is a detective
			return ImmutableSet.copyOf(allMoves);
		}

		private Player pieceToPlayer(Piece piece) {
			return everyone.stream()
					.filter(player -> player.piece() == piece)
					.findFirst()
					.orElse(null);
		}

		private void updateRemaining(Move move){
			Piece lastActed = move.commencedBy();
			Set<Piece> remains = new HashSet<>(remaining);
			remains.remove(lastActed);
			if (remains.size() == 0){
				//remaining is populated accordingly based on who played last
				if (lastActed.isMrX()){
					remains = detectives.stream()
						.filter(this::hasTickets)
						.map(Player::piece)
						.collect(Collectors.toSet());
				}
				if (lastActed.isDetective()){
					remains.add(mrX.piece());
				}
			}
			remaining = ImmutableSet.copyOf(remains);
		}

		private void updateTickets(Move move){
			Player player = pieceToPlayer(move.commencedBy());

			Function<SingleMove, Player> smf = sm -> player.use(sm.ticket).at(sm.destination);
			Function<DoubleMove, Player> dmf = dm -> {
				mrX = mrX.use(Ticket.DOUBLE).at(dm.source());
				mrX = mrX.use(dm.ticket1).at(dm.destination1).use(dm.ticket2).at(dm.destination2);
				return mrX;
			};

			Visitor<Player> visitor = new FunctionalVisitor<>(smf, dmf);

			// The visit method of the object in memory will be the visit method of the visitor that is passed in.
			// That visit method is passed a reference of the move which called it.

			// Since the functional visitor is overloaded with a visit method Singlemove and DoubleMove, the
			// correct visit method will be called, which applies either the smf or dmf function which
			// we pass in as arguments to the FunctionalVisitor.

			if (player.isMrX()){
				mrX = move.visit(visitor);
			}

			if (player.isDetective()) {
				Set<Player> updatedDet = new HashSet<>(detectives);
				updatedDet.remove(player);
				updatedDet.add(move.visit(visitor));
				mrX = mrX.give(move.tickets());
				detectives = ImmutableList.copyOf(updatedDet);
			}
		}

		private void updateLog(Move move){
			Function<SingleMove, ArrayList<LogEntry>> smf = sm -> {
				if (setup.rounds.get(log.size())) {
					return new ArrayList<>(List.of(LogEntry.reveal(sm.ticket, sm.destination)));
				} else {
					return new ArrayList<>(List.of(LogEntry.hidden(sm.ticket)));
				}};
			Function<DoubleMove, ArrayList<LogEntry>> dmf = dm -> {
				if (setup.rounds.get(log.size()) && setup.rounds.get(log.size()+1)) {
					return new ArrayList<>(List.of(LogEntry.reveal(dm.ticket1, dm.destination1),LogEntry.reveal(dm.ticket2, dm.destination2)));
				} else if (setup.rounds.get(log.size()+1)){
					return new ArrayList<>(List.of(LogEntry.hidden(dm.ticket1), LogEntry.reveal(dm.ticket2, dm.destination2)));
				} else if (setup.rounds.get(log.size())){
					return new ArrayList<>(List.of(LogEntry.reveal(dm.ticket1, dm.destination2), LogEntry.hidden(dm.ticket2)));
				} else {
					return new ArrayList<>(List.of(LogEntry.hidden(dm.ticket1), LogEntry.hidden(dm.ticket2)));
				}};

			Player player = pieceToPlayer(move.commencedBy());
			List<LogEntry> tempLog = new ArrayList<>(List.copyOf(log));

			// The visit method of the object in memory will be the visit method of the visitor that is passed in.
			// That visit method is passed a reference of the move which called it.

			// Since the functional visitor is overloaded with a visit method Singlemove and DoubleMove, the
			// correct visit method will be called, which applies either the smf or dmf function which
			// we pass in as arguments to the FunctionalVisitor.

			if  (player.isMrX()) {
				Visitor<ArrayList<LogEntry>> visitor = new FunctionalVisitor<>(smf,dmf);
				tempLog.addAll(move.visit(visitor));
			}
			log = ImmutableList.copyOf(tempLog);
		}

		@Nonnull @Override public GameState advance(Move move) {
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			// When a move is made, remaining, log and tickets are updated, and a new MyGameState is returned.
			updateRemaining(move);
			updateLog(move);
			updateTickets(move);
			return new MyGameState(setup, remaining, log, mrX, detectives);
		}
	}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}
}