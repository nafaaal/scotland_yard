package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.lang.reflect.Array;
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

			List<Player> allPlayers = new ArrayList<>();
			allPlayers.add(mrX);
			allPlayers.addAll(detectives);
			everyone = ImmutableList.copyOf(allPlayers);

			moves = getAvailableMoves();

			//Make into function
			if(log.size() == setup.rounds.size()) {
				winner = ImmutableSet.of(mrX.piece());
			} else if (isMrxCaught()) {
				winner = ImmutableSet.copyOf(detectivePieces());
				System.out.println(winner);
			} else {
				winner = ImmutableSet.of();
			}




			// detectives list cannot be empty.
			if ((detectives.size() == 0)) throw new NullPointerException();
			//  mrX cannot be null and check if Player mrX is actually MrX.
			if ((mrX == null) || mrX.isDetective()) throw new IllegalArgumentException();

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
		@Nonnull @Override public ImmutableSet<Piece> getWinner() {
			return winner;
		}

		private ImmutableSet<Piece> detectivePieces() {
			Set<Piece> dets = new HashSet<>();
			for (Player detective : detectives){
				dets.add(detective.piece());
			}
			return ImmutableSet.copyOf(dets);
		}

		@Nonnull @Override public ImmutableSet<Move> getAvailableMoves() {
			Set<Move> allMoves = new HashSet<>();
			Player nextToPlay = pieceToPlayer(remaining.iterator().next());
			if (nextToPlay == null) throw new IllegalArgumentException();
			if (nextToPlay.isMrX()) {
				allMoves = getAllMoves(setup, detectives, nextToPlay, nextToPlay.location());
			} else {
				for(Piece p : remaining){
					allMoves.addAll(getAllMoves(setup, detectives, pieceToPlayer(p), pieceToPlayer(p).location()));
				}
			}
			return ImmutableSet.copyOf(allMoves);
		}

		private ImmutableSet<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			Set<SingleMove> singleMoves = new HashSet<>();

			Set<Integer> detectiveLocations = new HashSet<>();
			for (Player detective : detectives){
				detectiveLocations.add(detective.location());
			}

			for(int destination : setup.graph.adjacentNodes(source)) {
				if (!(detectiveLocations.contains(destination))){
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
			ImmutableSet<SingleMove> secondMove;
			if (player.isMrX() && player.has(Ticket.DOUBLE) && (setup.rounds.size()-1 != log.size())){
				for (Move first : firstMove){
					player = player.use(first.tickets());
					secondMove = makeSingleMoves(setup,detectives,player,((SingleMove)first).destination);
					for (Move second : secondMove){
						doubleMoves.add(new DoubleMove(player.piece(), first.source(), ((SingleMove)first).ticket, ((SingleMove)first).destination, ((SingleMove)second).ticket, ((SingleMove)second).destination));
					}
					player = player.give(first.tickets());
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

		private ImmutableSet<Piece> createRemaining(Piece p){
			Set<Piece> remains = new HashSet<>(remaining);
			remains.remove(p); // Removes player which has acted already
			if (remains.size() == 0){ // Check if round is over
				if (p.isMrX()){ // Check if player was mrX (In the case of round 1)
					for (Player pl : detectives){ // Add all active detectives
						if (hasTickets(pl)) {
							remains.add(pl.piece());
						}
					}
				} // If last acted player was detective and size == 0, add mrX and start new round
				else remains.add(mrX.piece());
			}
			return ImmutableSet.copyOf(remains);
		}

		private void useTicket(Move m){
			Player p = pieceToPlayer(m.commencedBy());
			Iterable<Ticket> tickets_used = m.tickets();
			if (p.isMrX()){
				if (m instanceof SingleMove) {
					int d = ((SingleMove) m).destination;
					Ticket t  = ((SingleMove) m).ticket;
					mrX = p.use(t).at(d);
				}
				if (m instanceof DoubleMove) {
					int d1 = ((DoubleMove) m).destination1;
					Ticket t1  = ((DoubleMove) m).ticket1;
					int d2 = ((DoubleMove) m).destination2;
					Ticket t2  = ((DoubleMove) m).ticket2;
					mrX =  p.use(t1).at(d1);
					mrX =  p.use(t2).at(d2);
				}
			} else {
				Set<Player> dets = new HashSet<>();
				int d = ((SingleMove) m).destination;
				Ticket t  = ((SingleMove) m).ticket;
				for (Player pl : detectives) {
					if (m.commencedBy() == pl.piece()) dets.add(pl.use(t).at(d));
					else dets.add(pl);
				}
				detectives = ImmutableList.copyOf(dets);
			}
		}

		private void updateLocation(){
			//need to update location
		}

		private void updateLog(Move move, Player pl){
			// Need to append to current log and not overwrite
			List<LogEntry> tempLog = new ArrayList<>();
			if (pl.isMrX()) {
				if (move instanceof SingleMove) {
					tempLog.add(LogEntry.hidden(((SingleMove) move).ticket));
				}
				if (move instanceof DoubleMove) {
					tempLog.add(LogEntry.hidden(((DoubleMove) move).ticket1));
					tempLog.add(LogEntry.hidden(((DoubleMove) move).ticket2));
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
			// need to make move -> therefore update remaining(done), tickets and logs(doing)
			Player pl = pieceToPlayer(move.commencedBy());
			remaining = createRemaining(move.commencedBy());
			updateLog(move, pl);
			useTicket(move);

			return new MyGameState(setup, remaining, log, mrX, detectives);
		}
	}



	@Nonnull @Override public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

}

//Log needs to make non-hidden every 5th move
//Give det tickets to mrX
// Need to append to current log and not overwrite
//need to update location