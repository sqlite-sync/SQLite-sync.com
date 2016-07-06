using System;

namespace SQLiteSyncCOMLibXamarin
{
	public class Document
	{
		public int Id { get; private set; }

		public string Name { get; private set; }

		public int Size { get; private set; }

		public Document (int id, string name, int size)
		{
			Id = id;
			Name = name;
			Size = size;
		}
	}
}

