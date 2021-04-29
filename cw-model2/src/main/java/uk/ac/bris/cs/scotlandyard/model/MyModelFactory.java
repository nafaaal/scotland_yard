package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;

import java.util.ArrayList;
import java.util.List;

public final class MyModelFactory implements Factory<Model> {

	public final class GameModel implements Model{

		ImmutableSet<Observer> observers = ImmutableSet.of();
		GameState state;
		GameSetup setup;
		Player mrX;
		ImmutableList<Player> detectives;

		GameModel(GameSetup setup, Player mrX, ImmutableList<Player> detectives){
			this.setup = setup;
			this.mrX = mrX;
			this.detectives = detectives;
			state = new MyGameStateFactory().build(setup,mrX,detectives);
		}

		@Nonnull public Board getCurrentBoard() {
			return state;
		}

		public void registerObserver(@Nonnull Observer observer) {
			if (observer == null) throw new NullPointerException();
			if (observers.contains(observer)) throw new IllegalArgumentException();
			List<Observer> copyObservers = new ArrayList<>(observers);
			copyObservers.add(observer);
			observers = ImmutableSet.copyOf(copyObservers);
		}

		public void unregisterObserver(@Nonnull Observer observer) {
			if (observer == null) throw new NullPointerException();
			if (!(observers.contains(observer))) throw new IllegalArgumentException();
			List<Observer> copyObservers = new ArrayList<>(observers);
			copyObservers.remove(observer);
			observers = ImmutableSet.copyOf(copyObservers);
		}

		@Nonnull public ImmutableSet<Observer> getObservers() {
			return observers;
		}

		public void chooseMove(@Nonnull Move move) {
			Observer.Event event;
			state = state.advance(move);
			if (!state.getWinner().isEmpty()){
				event = Observer.Event.GAME_OVER;
			} else {
				event = Observer.Event.MOVE_MADE;
			}
			for (Observer obs : observers){
				obs.onModelChanged(getCurrentBoard(), event);
			}
		}
	}

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		return new GameModel(setup, mrX, detectives);
	}
}
