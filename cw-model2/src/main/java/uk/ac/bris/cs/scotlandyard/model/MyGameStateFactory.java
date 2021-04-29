package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.*;
import javax.annotation.Nonnull;
import java.util.stream.Collectors;

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
		private ImmutableList<Player> detectives;
		private ImmutableList<Player> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;


		private MyGameState(final GameSetup setup, final ImmutableSet<Piece> remaining, final ImmutableList<LogEntry> log, final Player mrX, final ImmutableList<Player> detectives) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

			setEveryone();
			setMoves();
			findWinner();
			validate();

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
			List<Piece> players = everyone
					.stream()
					.map(x -> x.piece())
					.collect(Collectors.toList());
			return ImmutableSet.copyOf(players);
		}

		@Nonnull @Override public Optional<Integer> getDetectiveLocation(Detective det) {
			for (Player detective : detectives) {
				if (detective.equals(pieceToPlayer(det))) return Optional.of(detective.location());
			}
			return Optional.empty();
		}

		@Nonnull @Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			for (Player player : everyone) {
				if (player.equals(pieceToPlayer(piece))) return Optional.of(new myBoard(player.tickets()));
			}
			return Optional.empty();

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

		private void setEveryone(){
			List<Player> allPlayers = new ArrayList<>();
			allPlayers.add(mrX);
			allPlayers.addAll(detectives);
			everyone = ImmutableList.copyOf(allPlayers);
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
			boolean gameRoundsReached = (log.size() == setup.rounds.size()) && this.remaining.contains(mrX.piece());
			if(gameRoundsReached || detEmptyTickets()) {
				winner = ImmutableSet.of(mrX.piece());
				moves = ImmutableSet.of();
			} else if (isMrxCaught() || isMrxStuck()) {
				winner = ImmutableSet.copyOf(detectivePieces());
				moves = ImmutableSet.of();
			} else {
				winner = ImmutableSet.of();
			}
		}

		private boolean detEmptyTickets(){
			int count = 0;
			for (Player det : detectives){
				ImmutableMap<Ticket, Integer> tickets = det.tickets();
				for (Map.Entry<Ticket, Integer> pair : tickets.entrySet()) {
					count +=pair.getValue();
				}
			}
			return count == 0;
		}

		private boolean isMrxStuck(){
			return (getmrXMoves().size() == 0 && remaining.contains(mrX.piece()));
		}

		private ImmutableSet<Piece> detectivePieces() {
			List<Piece> dets = detectives
					.stream()
					.map(x -> x.piece())
					.collect(Collectors.toList());
			return ImmutableSet.copyOf(dets);
		}

		private void setMoves() {
			Set<Move> allMoves = new HashSet<>();
			allMoves.addAll(getmrXMoves());
			allMoves.addAll(getDetMoves());
			moves = ImmutableSet.copyOf(allMoves);
		}

		private ImmutableSet<Move> getmrXMoves() {
			Set<Move> mrXMoves = new HashSet<>();
			Player nextToPlay = pieceToPlayer(remaining.iterator().next());
			if (nextToPlay == null) throw new IllegalArgumentException();
			if (nextToPlay.isMrX()) {
				mrXMoves = getAllMoves(setup, detectives, nextToPlay, nextToPlay.location());
			}
			return ImmutableSet.copyOf(mrXMoves);
		}

		private ImmutableSet<Move> getDetMoves() {
			Set<Move> detMoves = new HashSet<>();
				for(Piece piece : remaining){
					if (piece.isDetective()){
						detMoves.addAll(getAllMoves(setup, detectives, pieceToPlayer(piece), pieceToPlayer(piece).location()));
					}
			}
			return ImmutableSet.copyOf(detMoves);
		}


		private ImmutableSet<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			Set<SingleMove> singleMoves = new HashSet<>();

			Set<Integer> detLocations = new HashSet<>();	
			for (Player det : detectives){
				detLocations.add(det.location());
			}

			for(int destination : setup.graph.adjacentNodes(source)) {
				if (!(detLocations.contains(destination))){
					for(Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
						if (player.has(t.requiredTicket())) {
							singleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
						}
					}
					if (player.has(Ticket.SECRET)) {
						singleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
					}
				}
			}
			return ImmutableSet.copyOf(singleMoves);
		}

		private ImmutableSet<Move> getAllMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			Set<Move> firstMove = new HashSet<>(makeSingleMoves(setup, detectives, player, source));
			Set<Move> doubleMoves = new HashSet<>();
			Set<SingleMove> secondMove;
			if (player.has(Ticket.DOUBLE) && (setup.rounds.size()-1 != log.size())){
				for (Move first : firstMove){
					player = player.use(first.tickets()); // update tickets so that correct double moves can be found
					secondMove = makeSingleMoves(setup,detectives,player,((SingleMove)first).destination);
					for (Move second : secondMove){
						doubleMoves.add(new DoubleMove(player.piece(), first.source(), ((SingleMove)first).ticket, ((SingleMove)first).destination, ((SingleMove)second).ticket, ((SingleMove)second).destination));
					}
					player = player.give(first.tickets()); // change tickets back
				}
			}
			firstMove.addAll(doubleMoves);
			return ImmutableSet.copyOf(firstMove);
		}

		private Player pieceToPlayer(Piece piece) {
			Player result = null;
			for (Player player : everyone) {
				if (player.piece() == piece) {
					result = player;
				}
			}
			return result;
		}

		private boolean hasTickets(Player detective){
			boolean result = false;
			Map<Ticket,Integer> tickets = new HashMap<>(detective.tickets());
			for (int i : tickets.values()){
				if (i > 0) {
					result = true;
					break;
				}
			}
			return result;
		}

		private void createRemaining(Move m){
			Piece piece = m.commencedBy();
			Set<Piece> remains = new HashSet<>(remaining);
			remains.remove(piece); // Removes player which has acted already
			if (remains.size() == 0){ // Check if round is over
				if (piece.isMrX()){ // Check if player was mrX (In the case of round 1)
					for (Player det : detectives){ // Add all active detectives
						if (hasTickets(det)) {
							remains.add(det.piece());
						}
					}
				} // If last acted player was detective and size == 0, add mrX and start new round
				else remains.add(mrX.piece());
			}
			remaining = ImmutableSet.copyOf(remains);
		}

		private void updateTickets(Move m){
			Player copyOfPlayer = pieceToPlayer(m.commencedBy());
			if (copyOfPlayer.isMrX()){ // Have to check if mrX makes double move, and update accordingly.
				if (m instanceof SingleMove) {
					mrX = mrX.use(((SingleMove) m).ticket).at(((SingleMove) m).destination);
				}
				if (m instanceof DoubleMove) {
					mrX = mrX.use(Ticket.DOUBLE).at(m.source());
					mrX = mrX.use(((DoubleMove) m).ticket1).at(((DoubleMove) m).destination1);
					mrX = mrX.use(((DoubleMove) m).ticket2).at(((DoubleMove) m).destination2);
				}
			} else { // Detectives can only make a singleMove.
				Set<Player> updatedDetectives = new HashSet<>();
				for (Player copyOfDetective : detectives) {
					// change location, tickets of detective who made the move, and keeping others the same
					if (m.commencedBy() == copyOfDetective.piece()) {
						Ticket ticket = ((SingleMove) m).ticket;
						Player newDetective = copyOfDetective.use(((SingleMove) m).ticket).at(((SingleMove) m).destination);
						updatedDetectives.add(newDetective);
						// Give used ticket to mrX as part of rules
						mrX = mrX.give(ticket);
					} else {
						// if moves was not made by the detective, do not update tickets and location
						updatedDetectives.add(copyOfDetective);
					}
				}
				detectives = ImmutableList.copyOf(updatedDetectives);
			}
		}

		private void updateLog(Move move){
			Player player = pieceToPlayer(move.commencedBy());
			List<LogEntry> tempLog = new ArrayList<>(List.copyOf(log));
			if  (player.isMrX()) {
				if (move instanceof SingleMove) {
					if (setup.rounds.get(this.log.size())) {
						tempLog.add(LogEntry.reveal(((SingleMove) move).ticket, ((SingleMove) move).destination));
					} else {
						tempLog.add(LogEntry.hidden(((SingleMove) move).ticket));
					}
				}
				if (move instanceof DoubleMove) {
					if (setup.rounds.get(this.log.size()) && setup.rounds.get(this.log.size()+1)) {
						tempLog.add(LogEntry.reveal(((DoubleMove) move).ticket1, ((DoubleMove) move).destination1));
						tempLog.add(LogEntry.reveal(((DoubleMove) move).ticket2, ((DoubleMove) move).destination2));
					} else if (setup.rounds.get(this.log.size()+1)){
						tempLog.add(LogEntry.hidden(((DoubleMove) move).ticket1));
						tempLog.add(LogEntry.reveal(((DoubleMove) move).ticket2, ((DoubleMove) move).destination2));
					} else if (setup.rounds.get(this.log.size())){
						tempLog.add(LogEntry.reveal(((DoubleMove) move).ticket1, ((DoubleMove) move).destination1));
						tempLog.add(LogEntry.hidden(((DoubleMove) move).ticket2));
					} else {
						tempLog.add(LogEntry.hidden(((DoubleMove) move).ticket1));
						tempLog.add(LogEntry.hidden(((DoubleMove) move).ticket2));
					}
				}
			}
			log = ImmutableList.copyOf(tempLog);
		}

		private boolean isMrxCaught(){
			for (Player det : detectives) {
				if (det.location() == mrX.location()) {
					return true;
				}
			}
			return false;
		}

		@Nonnull @Override public GameState advance(Move move) {
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			createRemaining(move);
			updateLog(move);
			updateTickets(move);
			return new MyGameState(setup, remaining, log, mrX, detectives);
		}
	}

	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}
}