/*
Copyright (c) 2007-2009 Kristofer Karlsson <kristofer.karlsson@gmail.com>
and Jan Matejek <ja@matejcik.cz>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package se.krka.kahlua.stdlib;

import se.krka.kahlua.vm.KahluaArray;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableImpl;

import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaUtil;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaState;

public final class TableLib implements JavaFunction {

	private static final int CONCAT = 0;
	private static final int INSERT = 1;
	private static final int REMOVE = 2;
	private static final int MAXN = 3;
	private static final int NEWARRAY = 4;
	private static final int NUM_FUNCTIONS = 5;

	private static final String[] names;
	private static final TableLib[] functions;
	
	static {
		names = new String[NUM_FUNCTIONS];
		names[CONCAT] = "concat";
		names[INSERT] = "insert";
		names[REMOVE] = "remove";
		names[MAXN] = "maxn";
		names[NEWARRAY] = "newarray";
		functions = new TableLib[NUM_FUNCTIONS];
		for (int i = 0; i < NUM_FUNCTIONS; i++) {
			functions[i] = new TableLib(i);
		}
	}
	
	private final int index;

	public TableLib (int index) {
		this.index = index;
	}

	public static void register(KahluaTable environment) {
		KahluaTable table = new KahluaTableImpl();

		for (int i = 0; i < NUM_FUNCTIONS; i++) {
			table.rawset(names[i], functions[i]);
		}
		environment.rawset("table", table);
	}

	public String toString () {
		if (index < names.length) {
			return "table." + names[index];
		}
		return super.toString();
	}

	public int call (LuaCallFrame callFrame, int nArguments) {
		switch (index) {
			case CONCAT:
				return concat(callFrame, nArguments);
			case INSERT:
				return insert(callFrame, nArguments);
			case REMOVE:
				return remove(callFrame, nArguments);
			case MAXN:
				return maxn(callFrame, nArguments);
			case NEWARRAY:
				return newarray(callFrame, nArguments);
			default:
				return 0;
		}
	}

	private int newarray(LuaCallFrame callFrame, int arguments) {
		Object param = BaseLib.getOptArg(callFrame, 1, null);
		KahluaArray ret = new KahluaArray();
		if (param instanceof KahluaTable && arguments == 1) {
			KahluaTable t = (KahluaTable) param;
			int n = t.len();
			for (int i = n; i >= 1; i--) {
				ret.rawset(i, t.rawget(i));
			}
		} else {
            for (int i = arguments; i >= 1; i--) {
                ret.rawset(i, callFrame.get(i - 1));
            }
        }
		return callFrame.push(ret);
	}

	private static int concat (LuaCallFrame callFrame, int nArguments) {
		BaseLib.luaAssert(nArguments >= 1, "expected table, got no arguments");
		KahluaTable table = (KahluaTable)callFrame.get(0);

		String separator = "";
		if (nArguments >= 2) {
			separator = BaseLib.rawTostring(callFrame.get(1));
		}

		int first = 1;
		if (nArguments >= 3) {
			Double firstDouble = BaseLib.rawTonumber(callFrame.get(2));
			first = firstDouble.intValue();
		}

		int last;
		if (nArguments >= 4) {
			Double lastDouble = BaseLib.rawTonumber(callFrame.get(3));
			last = lastDouble.intValue();
		} else {
			last = table.len();
		}

		StringBuffer buffer = new StringBuffer();
		for (int i = first; i <= last; i++) {
			if (i > first) {
				buffer.append(separator);
			}

			Double key = KahluaUtil.toDouble(i);
			Object value = table.rawget(key);
			buffer.append(BaseLib.rawTostring(value));
		}

		return callFrame.push(buffer.toString());
	}
	
	public static void insert (LuaState state, KahluaTable table, Object element) {
		append(state, table, element);
	}

	public static void append(LuaState state, KahluaTable table, Object element) {
		int position = 1 + table.len();
		state.tableSet(table, KahluaUtil.toDouble(position), element);
	}

	public static void rawappend(KahluaTable table, Object element) {
		int position = 1 + table.len();
		table.rawset(KahluaUtil.toDouble(position), element);
	}

	public static void insert(LuaState state, KahluaTable table, int position, Object element) {
		int len = table.len();
		for (int i = len; i >= position; i--) {
			state.tableSet(table, KahluaUtil.toDouble(i+1), state.tableGet(table, KahluaUtil.toDouble(i)));
		}
		state.tableSet(table, KahluaUtil.toDouble(position), element);
	}

	public static void rawinsert(KahluaTable table, int position, Object element) {
		int len = table.len();
		if (position <= len) {
			Double dest = KahluaUtil.toDouble(len + 1);
			for (int i = len; i >= position; i--) {
				Double src = KahluaUtil.toDouble(i);
				table.rawset(dest, table.rawget(src));
				dest = src;
			}
			table.rawset(dest, element);
		} else {
			table.rawset(KahluaUtil.toDouble(position), element);
		}
	}

	private static int insert (LuaCallFrame callFrame, int nArguments) {
		BaseLib.luaAssert(nArguments >= 2, "Not enough arguments");
		KahluaTable t = (KahluaTable)callFrame.get(0);
		int pos = t.len() + 1;
		Object elem = null;
		if (nArguments > 2) {
			pos = BaseLib.rawTonumber(callFrame.get(1)).intValue();
			elem = callFrame.get(2);
		} else {
			elem = callFrame.get(1);
		}
		insert(callFrame.thread.state, t, pos, elem);
		return 0;
	}
	
	public static Object remove (LuaState state, KahluaTable table) {
		return remove(state, table, table.len());
	}
	
	public static Object remove (LuaState state, KahluaTable table, int position) {
		Object ret = state.tableGet(table, KahluaUtil.toDouble(position));
		int len = table.len();
		for (int i = position; i < len; i++) {
			state.tableSet(table, KahluaUtil.toDouble(i), state.tableGet(table, KahluaUtil.toDouble(i+1)));
		}
		state.tableSet(table, KahluaUtil.toDouble(len), null);
		return ret;
	}
	
	private static int remove (LuaCallFrame callFrame, int nArguments) {
		BaseLib.luaAssert(nArguments >= 1, "expected table, got no arguments");
		KahluaTable t = (KahluaTable)callFrame.get(0);
		int pos = t.len();
		if (nArguments > 1) {
			pos = BaseLib.rawTonumber(callFrame.get(1)).intValue();
		}
		callFrame.push(remove(callFrame.thread.state, t, pos));
		return 1;
	}
	
	private static int maxn (LuaCallFrame callFrame, int nArguments) {
		BaseLib.luaAssert(nArguments >= 1, "expected table, got no arguments");
		KahluaTable t = (KahluaTable)callFrame.get(0);
		Object key = null;
		int max = 0;
		while ((key = t.next(key)) != null) {
			if (key instanceof Double) {
				int what = (int) KahluaUtil.fromDouble(key);
				if (what > max) max = what;
			}
		}
		callFrame.push(KahluaUtil.toDouble(max));
		return 1;
	}
}