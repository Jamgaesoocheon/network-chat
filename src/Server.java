import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.LinkedList;

/**
 * ���� ������ �����ϱ� ���� LinkedList user�� �����մϴ�. ���� ��Ƽ�����带 �����ϰ� ������ �ޱ� ���� ServerSocekt��
 * listener�� �����մϴ� ���ο� ������ ������ ��Ƽ�����带 ���� �����ŵ�ϴ�.
 */
public class Server {

	private static LinkedList<UserInfo> users = new LinkedList<UserInfo>();

	public static void main(String[] args) throws Exception {
		System.out.println("The chat server is running...");
		ExecutorService pool = Executors.newFixedThreadPool(500);
		try (ServerSocket listener = new ServerSocket(9468)) {
			while (true) {
				pool.execute(new Handler(listener.accept()));
			}
		}
	}

	/**
	 * Ŭ���̾�Ʈ�� ó���ϴ� Ŭ�����Դϴ�.
	 */
	private static class Handler implements Runnable {
		private String name;
		private Socket socket;
		private InputStream in;
		private OutputStream out;
		private DataInputStream din;
		private DataOutputStream dout;

		/**
		 * Listener�� ���� ���� ���� Ŭ���̾�Ʈ�� HandlerŬ������ ���ϰ� �����մϴ�.
		 */
		public Handler(Socket socket) {
			this.socket = socket;
		}

		/**
		 * ������ �ۼ����� ���� Input,Output Stream���� �����ϰ�, �̸��� �о�ɴϴ�. �о�� �̸��� �ߺ��� �ִٸ� �ڿ� ��ȣ�� ����
		 * �ߺ��� �̸����� ǥ���ϰ� Ŭ���̾�Ʈ���� �� �̸��� ����϶�� �ǹ̷� �޽����� �����ϴ�. ���� �� �̸��� �������ݿ� �°� ������ ����� ���
		 * Ŭ���̾�Ʈ�鿡�� broadcast�� �մϴ� ���� ���ο� ���� ������ �����ϰ�, �ٸ� ���������� �������ݿ� �°� ������ �Ŀ� ���Ḯ��Ʈ��
		 * �߰��մϴ�. �߰��� ���� Ŭ���̾�Ʈ�鿡�� ���� ����Ʈ�� ������Ʈ �϶�� ������ broadcast�ϰ�, ���ο� �������Դ� ���� ����Ʈ��
		 * ToAll�� ������ �޽����� �����մϴ�. ���� �ݺ������� Ŭ���̾�Ʈ���Լ� ������ �о���� �׿� �´� �ൿ�� inMessage�Լ��� ����
		 * ���մϴ�. ������ ������ ��� ������������ �����ϰ� �����鿡�� �ش� ������ �������� �˸��� �޽����� broadcast�ϸ�, ������
		 * �ݽ��ϴ�.
		 */
		public void run() {
			try {
				boolean duplicate = false;

				in = socket.getInputStream();
				din = new DataInputStream(in);
				out = socket.getOutputStream();
				dout = new DataOutputStream(out);

				name = din.readUTF();
				if (!name.equals("ss")) {

					int i = 1;
					while (checkName(name)) {
						if (i == 1) {
							name = name + "(" + i + ")";
						} else {
							String str = "(" + Integer.toString(i - 1) + ")";
							name = name.replace(str, "(" + i + ")");
						}
						i++;
					}
					broadcast("NewUser/" + name);

					UserInfo user = new UserInfo(dout, name);

					for (UserInfo curuser : users) {
						dout.writeUTF("OldUser/" + curuser.getName());
					}

					users.add(user);

					broadcast("ListUpdate/new");
					dout.writeUTF("Set/e");

					while (true) {
						String input = din.readUTF();
						inMessage(input);
					}
				}
				else {}

			} catch (

			Exception e) {
				System.out.println(e);
			} finally {
				int i = 0;
				for (UserInfo user : users) {
					if (user.getName().equals(name) || user.getStream() == dout) {
						break;
					}
					i++;
				}
				if (dout != null) {
					users.remove(i);
				} else if (name != null) {
					try {
						broadcast("UserOut/" + name);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					users.remove(i);
				}
				for (UserInfo user : users) {
					try {
						user.getStream().writeUTF("UserOut/" + name);
						user.getStream().writeUTF("ListUpdate/quit");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				try {
					socket.close();
				} catch (IOException e) {
				}
			}

		}

		private boolean checkName(String str) {
			for (UserInfo user : users) {
				if (user.getName().equals(str)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * �о�� ���ڿ��� �������ݿ� �°� ó���ϴ� �Լ��Դϴ�. ���ڿ��� StringTokenizer�� ���� �� ���� ������, ù��°�� �޴� ���,
		 * �ι�°�� ������ ���, ����°�� �޽����� �����մϴ�. �޴� ����� ToALL�� ��� �޽����� �������ݿ� �°� ��� ��������
		 * broadcast�մϴ�. �޴� ����� �����Ǿ����� ���, �޴� ����� ������ ����� ���� �ʴٸ�, �������ݿ� �°� �޴� ���, ������
		 * ������� ���ڿ��� �����ϴ�.
		 */
		private void inMessage(String str) throws IOException {
			// TODO Auto-generated method stub
			StringTokenizer st = new StringTokenizer(str, "/");

			String Receiver = st.nextToken();
			String Sender = st.nextToken();
			String Message = st.nextToken();

			if (Receiver.equals("To ALL")) {
				broadcast("MSG/" + Sender + "/" + Message + "\n");
			} else {
				if (!Receiver.equals(Sender)) {
					for (UserInfo user : users) {
						if (user.getName().equals(Receiver)) {
							user.getStream().writeUTF("WhisperTo/" + Sender + "/" + Message + "\n");
						} else if (user.getName().equals(Sender)) {
							user.getStream().writeUTF("WhisperFrom/" + Receiver + "/" + Message + "\n");
						}
					}
				}
			}

		}

		/**
		 * broadcast�� �ϴ� �Լ��Դϴ� ����Ǿ��ִ� ���������� �ִ� ��� DataOutputStream���� ���ڿ��� �����մϴ�.
		 */
		private void broadcast(String string) throws IOException {
			for (UserInfo user : users) {
				user.getStream().writeUTF(string);
			}
		}
	}

	/**
	 * ���� ������ �޴� Ŭ�����Դϴ�. DataOutputStream������ �̸��� ����Ǿ�������, getName, getStream�Լ��� ����
	 * ������ �� �ֽ��ϴ�.
	 * 
	 */
	private static class UserInfo {
		private DataOutputStream out;
		private String name;

		UserInfo(DataOutputStream o, String st) {
			out = o;
			name = st;
		}

		UserInfo(DataOutputStream o) {
			out = o;
		}

		UserInfo(String st) {
			name = st;
		}

		public DataOutputStream getStream() {
			return out;
		}

		public String getName() {
			return name;
		}
	}

}