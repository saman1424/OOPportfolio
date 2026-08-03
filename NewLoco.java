package project;

import uk.ac.leedsbeckett.oop.GameWorld;
import uk.ac.leedsbeckett.oop.Locomotive;

public class NewLoco extends Locomotive {

    public NewLoco(GameWorld world, int x, int y) {
        super(world, x, y);
    }

    /**
     * Handles signal-based stopping mechanics for custom locomotives.
     *
     * @param signal Reason/type for train halt ("RED", "DANGER", etc.)
     */
    public void stopTrain11(String signal) {
        System.out.println("NewLoco received signal " + signal + ". Halting train.");
        setSpeed(0);
    }

	private void setSpeed(int i) {
		// TODO Auto-generated method stub
		
	}
}