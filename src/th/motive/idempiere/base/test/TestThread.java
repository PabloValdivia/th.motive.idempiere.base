package th.motive.idempiere.base.test;

public class TestThread {
	public static class MyThread extends Thread {
		private boolean status = true;
		@Override
		public void run() {
			super.run();
			int loop = 1;
			while (status) {
				System.out.println(String.valueOf(loop));
				loop++;
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		public void dostop () {
			status = false;
		}
	}
	
	public static void main (String [] arg) {
		MyThread t = new MyThread();
		t.start();
		try {
			MyThread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		t.dostop();
		System.out.println("stop");
		try {
			MyThread.sleep(50000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("done");
	}
}
