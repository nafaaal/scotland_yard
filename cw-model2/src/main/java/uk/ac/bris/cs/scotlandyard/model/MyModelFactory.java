package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;


public final class MyModelFactory implements Factory<Model> {

	public final class SomeModel implements Model{

		@Nonnull public Board getCurrentBoard() {
			return null;
		}

		public void registerObserver(@Nonnull Observer observer) {
			System.out.println("do something");
		}

		public void unregisterObserver(@Nonnull Observer observer) {
			System.out.println("do something");
		}


		@Nonnull public ImmutableSet<Observer> getObservers() {
			return null;
		}

		public void chooseMove(@Nonnull Move move) {
			System.out.println("do something");
		}
	}

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		// TODO
		throw new RuntimeException("Implement me!");
	}
}
