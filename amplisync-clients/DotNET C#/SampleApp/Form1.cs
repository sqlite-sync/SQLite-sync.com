using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.SQLite;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using SQLiteSyncCOMCsharp;

namespace SampleApp
{
    public partial class Form1 : Form
    {
        private string connString = "Data Source=DemoT.db;";
        private string wsUrl = "";

        public Form1()
        {
            InitializeComponent();
        }

        private void button1_Click(object sender, EventArgs e)
        {
            this.Cursor = Cursors.WaitCursor;
            wsUrl = txtServerUrl.Text;
            SQLiteSyncCOMClient sqlite = new SQLiteSyncCOMClient(connString, wsUrl);
            sqlite.ReinitializeDatabase(textBox1.Text);
            LoadData();
            this.Cursor = Cursors.Default;
            MessageBox.Show("Reinitialization done!");
        }

        private void button2_Click(object sender, EventArgs e)
        {
            this.Cursor = Cursors.WaitCursor;
            wsUrl = txtServerUrl.Text;
            SQLiteSyncCOMClient sqlite = new SQLiteSyncCOMClient(connString, wsUrl);
            sqlite.SendAndRecieveChanges(textBox1.Text);
            LoadData();
            this.Cursor = Cursors.Default;
            MessageBox.Show("Send and recieve changes done!");
        }

        private void Form1_Load(object sender, EventArgs e)
        {
            try
            {
                LoadData();
            }
            catch (Exception ex)
            {
                //database not existing
            }
        }

        private void LoadData()
        {
            using (SQLiteConnection conn = new SQLiteConnection(this.connString))
            {
                using (SQLiteCommand cmd = new SQLiteCommand())
                {
                    cmd.Connection = conn;
                    conn.Open();

                    SQLiteHelper sh = new SQLiteHelper(cmd);

                    dataGridView1.DataSource = sh.Select("Select * from document;");
                    dataGridView2.DataSource = sh.Select("Select * from user;");

                    conn.Close();
                }
            }
        }
    }
}
