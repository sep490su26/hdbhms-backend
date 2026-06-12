import java.sql.*;
public class DbCheck {
  public static void main(String[] args) throws Exception {
    String url = args[0], user = args[1], pass = args[2], paymentId = args[3];
    try (Connection c = DriverManager.getConnection(url, user, pass)) {
      String sql = "select pi.id, pi.provider, pi.status, pi.amount, pi.payment_content, pi.expires_at, pi.invoice_id, pi.deposit_agreement_id, " +
          "i.status invoice_status, i.remaining_amount, i.paid_amount, da.status deposit_status, da.room_hold_id, " +
          "r.id room_id, r.room_code, r.current_status room_status, rh.status hold_status, rh.expires_at hold_expires_at " +
          "from payment_intents pi " +
          "left join invoices i on i.id=pi.invoice_id " +
          "left join deposit_agreements da on da.id=pi.deposit_agreement_id " +
          "left join room_holds rh on rh.id=da.room_hold_id " +
          "left join rooms r on r.id=da.room_id " +
          "where pi.id=?";
      try (PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setLong(1, Long.parseLong(paymentId));
        try (ResultSet rs = ps.executeQuery()) {
          ResultSetMetaData md = rs.getMetaData();
          while (rs.next()) {
            for (int i=1; i<=md.getColumnCount(); i++) {
              System.out.println(md.getColumnLabel(i) + "=" + rs.getString(i));
            }
          }
        }
      }
    }
  }
}
