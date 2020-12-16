import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

public class client extends JFrame implements ActionListener, KeyListener {

	private Socket socket;
	private String IP;
	private int port;
	private InputStream in;
	private OutputStream out;
	private DataInputStream din;
	private DataOutputStream dout;
	private String nickname;

	Vector user_list = new Vector();
	StringTokenizer st;

	private JFrame Login;
	private JTextField IDText = new JTextField();
	JButton chatStart = new JButton("Start");
	KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);

	private JFrame frame;
	JButton sendBtn = new JButton("Send");
	JButton quitBtn = new JButton("Quit");
	JScrollPane scrollPane_1 = new JScrollPane();
	JScrollPane scrollPane = new JScrollPane();
	JTextArea messages = new JTextArea();
	private final JScrollPane scrollPane_2 = new JScrollPane();
	private final JList list = new JList();
	private final JTextField sendingText_1 = new JTextField();

	/*
	 * �����Լ����ϴ�. ���۽� ����GUI�� ������ �ʰ� �����մϴ�.
	 */
	public static void main(String[] args) {

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					client window = new client();
					window.frame.setVisible(false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/*
	 * ��Ʈ��ũ�� �����ϴ� �Լ��Դϴ�. configuration Ŭ������ ���� �о�� IP�� port��ȣ�� �´� ������ �����ϰ�, ����������
	 * �����Ǹ� connection�Լ��� �����մϴ�.
	 */
	private void network() {
		try {
			socket = new Socket(IP, port);
			if (socket != null) {
				connection();
			}
		} catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(null, "���� ����", "�˸�", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "���� ����", "�˸�", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * ��Ʈ��ũ �������� �����ϴ� �Լ��Դϴ�. ���Ͽ� �°� output,input stream���� �����ϰ� �α��� GUI�� �������ʰ� �ϰ�
	 * ����GUI�� ���̰� �����մϴ�. ���� �������� �ڽ��� �г����� �����ϰ�, �������Լ� �̸��� ���� �����޽��ϴ�. ��������Ʈ�� ��ο���
	 * �߼��ϴ� ToALL�� �߰��մϴ�. ���� �ݺ����� ���� �����κ��� ������ �о����, �װ��� inMessage�Լ��� �־� ó���մϴ�.
	 */
	private void connection() {

		try {
			out = socket.getOutputStream();
			in = socket.getInputStream();
			dout = new DataOutputStream(out);
			din = new DataInputStream(in);
		} catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(null, "���� ����", "�˸�", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "���� ����", "�˸�", JOptionPane.ERROR_MESSAGE);
		}

		sendMessage(nickname);

		user_list.add("To ALL");
		Thread thr = new Thread(new Runnable() {
			public void run() {
				if (!nickname.equals("ss")){
					change();
					messages.append("ä���� �ڽ��� �鿩�ٺ��� �ſ��Դϴ�. �ų� ä�ú�Ź�帳�ϴ�\n/�� �Է½� ������ �߻��� �� �ֽ��ϴ�.\n");
					while (true) {
						try {
							String msg = din.readUTF();
							inMessage(msg);
						} catch (IOException e1) {
							try {
								in.close();
								out.close();
								din.close();
								dout.close();
								socket.close();
								JOptionPane.showMessageDialog(null, "������ ���� ������", "�˸�", JOptionPane.ERROR_MESSAGE);
								break;
							} catch (IOException e2) {
							}
						}

					}
				}

			}
		});

		thr.start();

	}

	/*
	 * �޽����� ��������� Ȯ���ϰ� �׿� �°� �ൿ�ϴ� �Լ��Դϴ�. �о�� ���ڿ��� StringTokenizer�� ���� ������ �� ó�� ������
	 * Protocol�� �� �� �ൿ�� ���մϴ� 1. NewUser�� ��� ���ο� ������ ���������� ä��â�� ���� ���� ����Ʈ�� �߰��մϴ�.
	 * 2. OldUser�� ��� ���� ����Ʈ�� �߰��մϴ�. 3. MSG�� ��� ���ο� ��ū�� �Է¹ް� 2��°��ū(�̸�):
	 * 3��°��ū(�޽�������)�� ä��â�� ���ϴ�. 4. Whisper To�� ��� ���ο� ��ū�� �Է¹ް� 2��°��ū(�̸�)>>�ڽ��� �̸�
	 * 3��°��ū(�޽�������)�� ä��â�� ���ϴ�. 5. Whisper From�� ��� ���ο� ��ū�� �Է¹ް� �ڽ��� �̸� >>2��°��ū(�̸�)
	 * 3��°��ū(�޽�������)�� ä��â�� ���ϴ�. 6. UserOut�� ��� ���� ����Ʈ���� �����ϰ� ����޽����� ���ϴ�. 7. Set�� ���
	 * ���ø�Ͽ��� 0�� �����մϴ� (To ALL)
	 */
	private void inMessage(String str) {

		st = new StringTokenizer(str, "/");

		String protocol = st.nextToken();
		String message = st.nextToken();
		String msg;

		if (protocol.equals("NewUser")) {
			user_list.add(message);
			messages.append(message + "���� ���� �����߽��ϴ�.\n");
			scrollPane_1.getVerticalScrollBar().setValue(scrollPane_1.getVerticalScrollBar().getMaximum());
		} else if (protocol.equals("OldUser")) {
			user_list.add(message);
		} else if (protocol.equals("MSG")) {
			String user = message;
			msg = st.nextToken();
			messages.append(user + ": " + msg);
			scrollPane_1.getVerticalScrollBar().setValue(scrollPane_1.getVerticalScrollBar().getMaximum());
		} else if (protocol.equals("WhisperTo")) {
			String user = message;
			msg = st.nextToken();
			messages.append(user + ">>" + nickname + ": " + msg);
			scrollPane_1.getVerticalScrollBar().setValue(scrollPane_1.getVerticalScrollBar().getMaximum());
		} else if (protocol.equals("WhisperFrom")) {
			String user = message;
			msg = st.nextToken();
			messages.append(nickname + ">>" + user + ": " + msg);
			scrollPane_1.getVerticalScrollBar().setValue(scrollPane_1.getVerticalScrollBar().getMaximum());
		} else if (protocol.equals("UserOut")) {
			user_list.remove(message);
			messages.append(message + "���� �����ϼ̽��ϴ�.\n");
		} else if (protocol.equals("ListUpdate")) {
			list.setListData(user_list);
		} else if (protocol.equals("Set")) {
			list.setSelectedIndex(0);
		}

	}

	/*
	 * �޽����� �����ϴ� �Լ��Դϴ�. ���� ���ڿ��� ������ DataOutputStream���� �������ϴ�.
	 */
	private void sendMessage(String str) {
		try {
			dout.writeUTF(str);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "���� ����", "�˸�", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/*
	 * Ŭ���� ������ �Դϴ�. configuration Ŭ������ �����ϰ� IP�ּҿ� port��ȣ�� �о�ɴϴ�. ���� GUI�� Ȱ��ȭ ��Ű��
	 * start�Լ��� �����մϴ�.
	 */
	public client() throws FileNotFoundException, IOException {
		configuration Info = new configuration();
		IP = Info.getIP();
		port = Info.getPort();
		LoginInit();
		MainInit();
		start();
	}

	/*
	 * Login GUI�����Դϴ�.
	 */
	private void LoginInit() {

		Login = new JFrame();
		Login.setBounds(100, 100, 450, 300);
		Login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Login.getContentPane().setLayout(null);

		IDText = new JTextField();
		IDText.setFont(new Font("����", Font.PLAIN, 30));
		IDText.setBounds(204, 74, 218, 42);
		Login.getContentPane().add(IDText);
		IDText.setColumns(10);

		chatStart.setFont(new Font("����", Font.BOLD, 30));
		chatStart.setBounds(151, 182, 116, 39);
		Login.getContentPane().add(chatStart);

		JLabel lblNewLabel = new JLabel("Nick Name");
		lblNewLabel.setFont(new Font("����", Font.BOLD, 30));
		lblNewLabel.setBounds(12, 74, 193, 45);
		Login.getContentPane().add(lblNewLabel);

		Login.setLocationRelativeTo(null);

	}

	/*
	 * Main GUI�����Դϴ�.
	 */
	private void MainInit() {

		sendingText_1.setFont(new Font("����", Font.BOLD, 16));
		sendingText_1.setColumns(10);

		frame = new JFrame();
		frame.setBounds(100, 100, 840, 840 / 12 * 9);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		sendBtn.setFont(new Font("����", Font.BOLD, 14));
		sendBtn.setBounds(687, 439, 114, 61);
		frame.getContentPane().add(sendBtn);

		quitBtn.setFont(new Font("����", Font.BOLD, 14));
		quitBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		quitBtn.setBounds(687, 520, 114, 61);
		frame.getContentPane().add(quitBtn);

		scrollPane_1.setBounds(24, 10, 775, 416);
		frame.getContentPane().add(scrollPane_1);
		messages.setLineWrap(true);
		messages.setFont(new Font("Monospaced", Font.BOLD, 16));
		messages.setEditable(false);
		scrollPane_1.setViewportView(messages);
		scrollPane_2.setBounds(142, 439, 525, 142);

		frame.getContentPane().add(scrollPane_2);

		scrollPane_2.setViewportView(sendingText_1);

		list.setFont(new Font("����", Font.BOLD, 16));
		list.setBounds(24, 439, 106, 134);
		list.setListData(user_list);

		frame.getContentPane().add(list);

		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Login.setVisible(true);
		Login.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setVisible(false);
	}

	/*
	 * GUI ������ҵ��� ActionListener�� �߰��ϴ� �Լ��Դϴ�.
	 */
	private void start() {
		chatStart.addActionListener(this);
		sendBtn.addActionListener(this);
		quitBtn.addActionListener(this);
		sendingText_1.addKeyListener(this);
	}

	/*
	 * ActionEvent�� �߻����� �� ó���ϴ� �Լ��Դϴ�. chatStart ��ư�� ������ ��� �̸��� ���̰� 0�� �ƴϰų�, null��
	 * �ƴϸ� network�� �����մϴ�. sendBtn�� ������ ���, ������ ���̰� 0�� �ƴϰ�, ����� ���õ��� ���� ��츦 �����ϰ�
	 * �޽����� �����ϴ�. �޽����� �޴»���̸�/�����»���̸�/���ڳ��� ���� �����Ǿ������� �������� �ؽ�Ʈâ�� ���ϴ�. quitBtn�� ������
	 * ���, ������ �ݰ� �ý����� �����մϴ�.
	 */
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == chatStart) {
			nickname = IDText.getText();
			if (IDText.getText().length() == 0 || IDText.getText().equals("null")) {
				IDText.requestFocus();
			} else {
				network();
			}
		} else if (e.getSource() == sendBtn && sendingText_1.getText().length() != 0
				&& !list.getSelectedValue().equals("null")) {
			String msg = sendingText_1.getText();
			String Receiver = (String) list.getSelectedValue();
			String Sender = this.nickname;
			sendMessage(Receiver + "/" + Sender + "/" + msg);
			sendingText_1.setText(null);
			sendingText_1.requestFocus();
		} else if (e.getSource() == quitBtn) {
			try {
				socket.close();
				System.exit(0);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}

	}

	public void keyPressed(KeyEvent arg0) {

	}

	/*
	 * Ű���尡 ������ �� ó���ϴ� �Լ��Դϴ�. actionPerformed�Լ����� sendBtn�� ������ ���� �����ϰ� �޽����� �����մϴ�.
	 */
	public void keyReleased(KeyEvent arg0) {
		if (arg0.getKeyCode() == KeyEvent.VK_ENTER && sendingText_1.getText().length() != 0
				&& !list.getSelectedValue().equals("null")) {
			String msg = sendingText_1.getText();
			String Receiver = (String) list.getSelectedValue();
			String Sender = this.nickname;
			sendMessage(Receiver + "/" + Sender + "/" + msg);
			sendingText_1.setText(null);
			sendingText_1.requestFocus();
		}
	}

	public void keyTyped(KeyEvent arg0) {

	}

	public void change() {
		this.frame.setVisible(true);
		this.Login.setVisible(false);
	}

}
