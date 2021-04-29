package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.ArrayList;
import java.util.List;

public final class MyModelFactory implements Factory<Model> {

	public final class SomeModel implements Model{

		ImmutableSet<Observer> observers = ImmutableSet.of();

		@Nonnull public Board getCurrentBoard() {
			return null;
		}

		public void registerObserver(@Nonnull Observer observer) {
			if (observer == null) throw new NullPointerException();
			List<Observer> copyObservers = new ArrayList<>(observers);
			copyObservers.add(observer);
			observers = ImmutableSet.copyOf(copyObservers);
		}

		public void unregisterObserver(@Nonnull Observer observer) {
			if (observer == null) throw new NullPointerException();
			List<Observer> copyObservers = new ArrayList<>(observers);
			copyObservers.remove(observer);
			observers = ImmutableSet.copyOf(copyObservers);

		}


		@Nonnull public ImmutableSet<Observer> getObservers() {
			return observers;
		}

		public void chooseMove(@Nonnull Move move) {
			System.out.println("do something");
		}
	}

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		return new SomeModel();
	}
}
