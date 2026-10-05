"""Exercise the conditional UPDATE on an in-memory SQL database; no production access."""
import sqlite3
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SQL = ET.parse(ROOT / "apps/api/src/main/resources/mapper/OrderMapper.xml").getroot().find("update[@id='hideById']").text
for key in ("id", "userId", "status"):
    SQL = SQL.replace("#{" + key + "}", ":" + key)

class OrderHideSqlTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.db.executescript("""
            CREATE TABLE tb_orders(id INTEGER, user_id INTEGER, status INTEGER, deleted INTEGER,
                publisher_hidden INTEGER, order_number TEXT, pay_amount NUMERIC);
            CREATE TABLE tb_payment_log(order_number TEXT);
            CREATE TABLE tb_refund_info(order_number TEXT, refund_status TEXT, total_fee INTEGER, refund INTEGER);
            CREATE TABLE tb_wx_transfer_log(order_number TEXT, state TEXT, transfer_amount INTEGER);
            INSERT INTO tb_orders VALUES(1,42,4,0,0,'O1',1.00);
        """)
    def tearDown(self):
        self.db.close()
    def hide(self, status=4, owner=42):
        return self.db.execute(SQL, {"id":1,"userId":owner,"status":status}).rowcount
    def test_cancelled_order_only_hides_once_for_owner(self):
        self.assertEqual(0,self.hide(owner=99))
        self.assertEqual(1,self.hide())
        self.assertEqual(0,self.hide())
        self.assertEqual((0,1),self.db.execute("SELECT deleted,publisher_hidden FROM tb_orders").fetchone())
    def test_inflight_or_changed_state_cannot_hide(self):
        for status in (-4,-2,-1,0,1,2,3,5,7,99):
            with self.subTest(status=status):
                self.db.execute("UPDATE tb_orders SET status=?",(status,))
                self.assertEqual(0,self.hide(status=status))
        self.assertEqual(0,self.hide(status=4))
    def test_cancelled_paid_order_is_retained(self):
        self.db.execute("INSERT INTO tb_payment_log VALUES('O1')")
        self.assertEqual(0,self.hide())
    def test_unsettled_funds_block_hiding(self):
        for state in (None,'REQUESTED','PROCESSING','REQUEST_FAILED','ABNORMAL','CLOSED'):
            with self.subTest(refund=state):
                self.db.execute("DELETE FROM tb_refund_info")
                self.db.execute("INSERT INTO tb_refund_info VALUES('O1',?,100,100)",(state,))
                self.assertEqual(0,self.hide())
        self.db.execute("DELETE FROM tb_refund_info")
        for state in (None,'CREATED','WAIT_USER_CONFIRM','QUERY_REQUIRED','FAIL','CANCELLED'):
            with self.subTest(transfer=state):
                self.db.execute("DELETE FROM tb_wx_transfer_log")
                self.db.execute("INSERT INTO tb_wx_transfer_log VALUES('O1',?,100)",(state,))
                self.assertEqual(0,self.hide())
    def test_refund_terminal_requires_full_success_and_preserves_ledger(self):
        self.db.execute("UPDATE tb_orders SET status=-3")
        self.assertEqual(0,self.hide(status=-3))
        self.db.execute("INSERT INTO tb_refund_info VALUES('O1','SUCCESS',100,90)")
        self.assertEqual(0,self.hide(status=-3))
        self.db.execute("UPDATE tb_refund_info SET refund=100")
        self.db.execute("INSERT INTO tb_payment_log VALUES('O1')")
        self.assertEqual(1,self.hide(status=-3))
        self.assertEqual(('SUCCESS',100,100),self.db.execute("SELECT refund_status,total_fee,refund FROM tb_refund_info").fetchone())
        self.assertEqual(1,self.db.execute("SELECT COUNT(*) FROM tb_payment_log").fetchone()[0])
    def test_transfer_terminal_requires_success_and_preserves_ledger(self):
        self.db.execute("UPDATE tb_orders SET status=6")
        self.assertEqual(0,self.hide(status=6))
        self.db.execute("INSERT INTO tb_wx_transfer_log VALUES('O1','SUCCESS',100)")
        self.assertEqual(1,self.hide(status=6))
        self.assertEqual(('SUCCESS',100),self.db.execute("SELECT state,transfer_amount FROM tb_wx_transfer_log").fetchone())
        self.assertEqual(0,self.db.execute("SELECT deleted FROM tb_orders").fetchone()[0])

if __name__ == '__main__':
    unittest.main()
