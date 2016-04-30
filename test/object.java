

public class object {

	// static int i;
	// static {
	// 	i = 10;
	// }

	// static void say() {
	// 	System.out.println(i);
	// }

	private class Tuple {

		public final String url;
		public int depth;

		public Tuple(String s, int depth) {
			this.url = s;
			this.depth = depth;
		}

		@Override  
        public int hashCode() {  

        	return url.hashCode();
        }

        @Override  
        public boolean equals(Object obj) {  

            if(!(obj instanceof Tuple)){  
                return false;  
            }  
            if(obj == this){  
                return true;  
            }  
            return this.url.equals(((Tuple)obj).url);  
        }  
	}

	public void test() {

		Tuple t1 = new Tuple("abc", 1);
		Tuple t2 = new Tuple("abc", 2);

		System.out.println((t1==null ? t2==null : t1.equals(t2)));	
	}

	public object(Object o) {
		this.o = o;
	}
	private Object o;

	public synchronized void notify() {
		
	}


	public static void main(String args[]) {
		new object().test();
	}
}