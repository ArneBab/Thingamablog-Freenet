/*
 * SimpleFieldSet.java
 *
 * Created on 16 avril 2008, 22:23
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package thingamablog.l10n;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import thingamablog.l10n.FSParseException;
import thingamablog.l10n.LineReader;
import net.sf.thingamablog.util.io.Closer;

/**
 * @author amphibian
 *
 * Very very simple FieldSet type thing, which uses the standard
 * Java facilities.
 */
public class SimpleFieldSet {
    
    private final Map values;
    private Map subsets;
    private String endMarker;
    private final boolean shortLived;
    static public final char MULTI_LEVEL_CHAR = '.';
    static public final char MULTI_VALUE_CHAR = ';';
    static public final char KEYVALUE_SEPARATOR_CHAR = '=';
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private Logger logger = Logger.getLogger("net.sf.thingamablog.L10n");
    
    /**
     * Create a SimpleFieldSet.
     * @param shortLived If false, strings will be interned to ensure that they use as
     * little memory as possible. Only set to true if the SFS will be short-lived or
     * small.
     */
    public SimpleFieldSet(boolean shortLived) {
        values = new HashMap();
        subsets = null;
        this.shortLived = shortLived;
    }
    
    /**
     * Construct a SimpleFieldSet from reading a BufferedReader.
     * @param br
     * @param allowMultiple If true, multiple lines with the same field name will be
     * combined; if false, the constructor will throw.
     * @param shortLived If false, strings will be interned to ensure that they use as
     * little memory as possible. Only set to true if the SFS will be short-lived or
     * small.
     * @throws IOException If the buffer could not be read, or if there was a formatting
     * problem.
     */
    public SimpleFieldSet(BufferedReader br, boolean allowMultiple, boolean shortLived) throws IOException {
        this(shortLived);
        read(br, allowMultiple);
    }
    
    public SimpleFieldSet(SimpleFieldSet sfs){
        values = new HashMap(sfs.values);
        if(sfs.subsets != null)
            subsets = new HashMap(sfs.subsets);
        this.shortLived = false; // it's been copied!
        endMarker = sfs.endMarker;
    }
    
    public SimpleFieldSet(LineReader lis, int maxLineLength, int lineBufferSize, boolean tolerant, boolean utf8OrIso88591, boolean allowMultiple, boolean shortLived) throws IOException {
        this(shortLived);
        read(lis, maxLineLength, lineBufferSize, tolerant, utf8OrIso88591, allowMultiple);
    }
    
    /**
     * Construct from a string.
     * String format:
     * blah=blah
     * blah=blah
     * End
     * @param shortLived If false, strings will be interned to ensure that they use as
     * little memory as possible. Only set to true if the SFS will be short-lived or
     * small.
     * @throws IOException if the string is too short or invalid.
     */
    public SimpleFieldSet(String content, boolean allowMultiple, boolean shortLived) throws IOException {
        this(shortLived);
        StringReader sr = new StringReader(content);
        BufferedReader br = new BufferedReader(sr);
        read(br, allowMultiple);
    }
    
    /**
     * Read from disk
     * Format:
     * blah=blah
     * blah=blah
     * End
     * @param allowMultiple
     */
    private void read(BufferedReader br, boolean allowMultiple) throws IOException {
        boolean firstLine = true;
        while(true) {
            String line = br.readLine();
            if(line == null) {
                if(firstLine) throw new EOFException();
                throw new IOException("No end Marker!");
            }
            firstLine = false;
            int index = line.indexOf(KEYVALUE_SEPARATOR_CHAR);
            if(index >= 0) {
                // Mapping
                String before = line.substring(0, index);
                String after = line.substring(index+1);
                if(!shortLived) after = after.intern();
                put(before, after, allowMultiple, false);
            } else {
                endMarker = line;
                return;
            }
            
        }
    }
    
    /**
     * Read from disk
     * Format:
     * blah=blah
     * blah=blah
     * End
     * @param utfOrIso88591 If true, read as UTF-8, otherwise read as ISO-8859-1.
     */
    private void read(LineReader br, int maxLength, int bufferSize, boolean tolerant, boolean utfOrIso88591, boolean allowMultiple) throws IOException {
        boolean firstLine = true;
        while(true) {
            String line = br.readLine(maxLength, bufferSize, utfOrIso88591);
            if(line == null) {
                if(firstLine) throw new EOFException();
                if(tolerant)
                    logger.log(Level.SEVERE, "No end marker");
                else
                    throw new IOException("No end marker");
                return;
            }
            if((line.length() == 0) && tolerant) continue; // ignore
            firstLine = false;
            int index = line.indexOf(KEYVALUE_SEPARATOR_CHAR);
            if(index >= 0) {
                // Mapping
                String before = line.substring(0, index);
                String after = line.substring(index+1);
                if(!shortLived) after = after.intern();
                put(before, after, allowMultiple, false);
            } else {
                endMarker = line;
                return;
            }
            
        }
    }
    
    public synchronized String get(String key) {
        int idx = key.indexOf(MULTI_LEVEL_CHAR);
        if(idx == -1)
            return (String) values.get(key);
        else if(idx == 0)
            return (subset("") == null) ? null : subset("").get(key.substring(1));
        else {
            if(subsets == null) return null;
            String before = key.substring(0, idx);
            String after = key.substring(idx+1);
            SimpleFieldSet fs = (SimpleFieldSet) (subsets.get(before));
            if(fs == null) return null;
            return fs.get(after);
        }
    }
    
    public String[] getAll(String key) {
        String k = get(key);
        if(k == null) return null;
        return split(k);
    }
    
    private static final String[] split(String string) {
        if(string == null) return new String[0];
        return string.split(String.valueOf(MULTI_VALUE_CHAR)); // slower???
//    	int index = string.indexOf(';');
//    	if(index == -1) return null;
//    	Vector v=new Vector();
//    	v.removeAllElements();
//        while(index>0){
//            // Mapping
//            String before = string.substring(0, index);
//            String after = string.substring(index+1);
//            v.addElement(before);
//            string=after;
//            index = string.indexOf(';');
//        }
//
//    	return (String[]) v.toArray();
    }
    
    private static final String unsplit(String[] strings) {
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<strings.length;i++) {
            if(i != 0) sb.append(MULTI_VALUE_CHAR);
            sb.append(strings[i]);
        }
        return sb.toString();
    }
    
    /**
     * Put contents of a fieldset, overwrite old values.
     */
    public void putAllOverwrite(SimpleFieldSet fs) {
        Iterator i = fs.values.keySet().iterator();
        while(i.hasNext()) {
            String key = (String) i.next();
            String hisVal = (String) fs.values.get(key);
            values.put(key, hisVal); // overwrite old
        }
        if(fs.subsets == null) return;
        if(subsets == null) subsets = new HashMap();
        i = fs.subsets.keySet().iterator();
        while(i.hasNext()) {
            String key = (String) i.next();
            SimpleFieldSet hisFS = (SimpleFieldSet) fs.subsets.get(key);
            SimpleFieldSet myFS = (SimpleFieldSet) subsets.get(key);
            if(myFS != null) {
                myFS.putAllOverwrite(hisFS);
            } else {
                subsets.put(key, hisFS);
            }
        }
    }
    
    /**
     * Set a key to a value. If the value already exists, throw IllegalStateException.
     * @param key The key.
     * @param value The value.
     */
    public void putSingle(String key, String value) {
        if(value == null) return;
        if(!shortLived) value = value.intern();
        if(!put(key, value, false, false))
            throw new IllegalStateException("Value already exists: "+value+" but want to set "+key+" to "+value);
    }
    
    /**
     * Aggregating put. Set a key to a value, if the value already exists, append to it.
     * @param key The key.
     * @param value The value.
     */
    public void putAppend(String key, String value) {
        if(value == null) return;
        if(!shortLived) value = value.intern();
        put(key, value, true, false);
    }
    
    /**
     * Set a key to a value, overwriting any existing value if present.
     * @param key The key.
     * @param value The value.
     */
    public void putOverwrite(String key, String value) {
        if(value == null) return;
        if(!shortLived) value = value.intern();
        put(key, value, false, true);
    }
    
    /**
     * Set a key to a value.
     * @param key The key.
     * @param value The value.
     * @param allowMultiple If true, if the key already exists then the value will be
     * appended to the existing value. If false, we return false to indicate that the
     * old value is unchanged.
     * @return True unless allowMultiple was false and there was a pre-existing value,
     * or value was null.
     */
    private synchronized final boolean put(String key, String value, boolean allowMultiple, boolean overwrite) {
        int idx;
        if(value == null) return true; // valid no-op
        if(value.indexOf('\n') != -1) throw new IllegalArgumentException("A simplefieldSet can't accept newlines !");
        if((idx = key.indexOf(MULTI_LEVEL_CHAR)) == -1) {
            String x = (String) values.get(key);
            
            if(!shortLived) key = key.intern();
            if(x == null || overwrite) {
                values.put(key, value);
            } else {
                if(!allowMultiple) return false;
                values.put(key, ((String)values.get(key))+ MULTI_VALUE_CHAR +value);
            }
        } else {
            String before = key.substring(0, idx);
            String after = key.substring(idx+1);
            SimpleFieldSet fs = null;
            if(subsets == null)
                subsets = new HashMap();
            fs = (SimpleFieldSet) (subsets.get(before));
            if(fs == null) {
                fs = new SimpleFieldSet(shortLived);
                if(!shortLived) before = before.intern();
                subsets.put(before, fs);
            }
            fs.put(after, value, allowMultiple, overwrite);
        }
        return true;
    }
    
    public void put(String key, int value) {
        // Use putSingle so it does the intern check
        putSingle(key, Integer.toString(value));
    }
    
    public void put(String key, long value) {
        putSingle(key, Long.toString(value));
    }
    
    public void put(String key, short value) {
        putSingle(key, Short.toString(value));
    }
    
    public void put(String key, char c) {
        putSingle(key, Character.toString(c));
    }
    
    public void put(String key, boolean b) {
        // Don't use putSingle, avoid intern check (Boolean.toString returns interned strings anyway)
        put(key, Boolean.toString(b), false, false);
    }
    
    public void put(String key, double windowSize) {
        putSingle(key, Double.toString(windowSize));
    }
    
    /**
     * Write the contents of the SimpleFieldSet to a Writer.
     * Note: The caller *must* buffer the writer to avoid lousy performance!
     * (StringWriter is by definition buffered, otherwise wrap it in a BufferedWriter)
     *
     * @warning keep in mind that a Writer is not necessarily UTF-8!!
     */
    public void writeTo(Writer w) throws IOException {
        writeTo(w, "", false);
    }
    
    /**
     * Write the contents of the SimpleFieldSet to a Writer.
     * Note: The caller *must* buffer the writer to avoid lousy performance!
     * (StringWriter is by definition buffered, otherwise wrap it in a BufferedWriter)
     *
     * @warning keep in mind that a Writer is not necessarily UTF-8!!
     */
    synchronized void writeTo(Writer w, String prefix, boolean noEndMarker) throws IOException {
        for(Iterator i = values.entrySet().iterator();i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            w.write(prefix);
            w.write(key);
            w.write(KEYVALUE_SEPARATOR_CHAR);
            w.write(value);
            w.write('\n');
        }
        if(subsets != null) {
            for(Iterator i = subsets.entrySet().iterator();i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                String key = (String) entry.getKey();
                SimpleFieldSet subset = (SimpleFieldSet) entry.getValue();
                if(subset == null) throw new NullPointerException();
                subset.writeTo(w, prefix+key+MULTI_LEVEL_CHAR, true);
            }
        }
        if(!noEndMarker) {
            if(endMarker == null)
                w.write("End\n");
            else {
                w.write(endMarker);
                w.write('\n');
            }
        }
    }
    
    public void writeToOrdered(Writer w) throws IOException {
        writeToOrdered(w, "", false);
    }
    
    private synchronized void writeToOrdered(Writer w, String prefix, boolean noEndMarker) throws IOException {
        String[] keys = (String[]) values.keySet().toArray(new String[values.size()]);
        int i=0;
        
        // Sort
        Arrays.sort(keys);
        
        // Output
        for(i=0; i < keys.length; i++)
            w.write(prefix+keys[i]+KEYVALUE_SEPARATOR_CHAR+get(keys[i])+'\n');
        
        if(subsets != null) {
            String[] orderedPrefixes = (String[]) subsets.keySet().toArray(new String[subsets.size()]);
            // Sort
            Arrays.sort(orderedPrefixes);
            
            for(i=0; i < orderedPrefixes.length; i++) {
                SimpleFieldSet subset = subset(orderedPrefixes[i]);
                if(subset == null) throw new NullPointerException();
                subset.writeToOrdered(w, prefix+orderedPrefixes[i]+MULTI_LEVEL_CHAR, true);
            }
        }
        
        if(!noEndMarker) {
            if(endMarker == null)
                w.write("End\n");
            else
                w.write(endMarker+ '\n');
        }
    }
    
    public String toString() {
        StringWriter sw = new StringWriter();
        try {
            writeTo(sw);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "WTF?!: "+e+" in toString()!", e);
        }
        return sw.toString();
    }
    
    public String toOrderedString() {
        StringWriter sw = new StringWriter();
        try {
            writeToOrdered(sw);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "WTF?!: "+e+" in toString()!", e);
        }
        return sw.toString();
    }
    
    public String getEndMarker() {
        return endMarker;
    }
    
    public void setEndMarker(String s) {
        endMarker = s;
    }
    
    public synchronized SimpleFieldSet subset(String key) {
        if(subsets == null) return null;
        int idx = key.indexOf(MULTI_LEVEL_CHAR);
        if(idx == -1)
            return (SimpleFieldSet) subsets.get(key);
        String before = key.substring(0, idx);
        String after = key.substring(idx+1);
        SimpleFieldSet fs = (SimpleFieldSet) subsets.get(before);
        if(fs == null) return null;
        return fs.subset(after);
    }
    
    /**
     * Like subset(), only throws instead of returning null.
     * @throws FSParseException
     */
    public synchronized SimpleFieldSet getSubset(String key) throws FSParseException {
        SimpleFieldSet fs = subset(key);
        if(fs == null) throw new FSParseException("No such subset "+key);
        return fs;
    }
    
    public Iterator keyIterator() {
        return new KeyIterator("");
    }
    
    public KeyIterator keyIterator(String prefix) {
        return new KeyIterator(prefix);
    }
    
    public Iterator toplevelKeyIterator() {
        return values.keySet().iterator();
    }
    
    public class KeyIterator implements Iterator {
        
        final Iterator valuesIterator;
        final Iterator subsetIterator;
        KeyIterator subIterator;
        String prefix;
        
        /**
         * It provides an iterator for the SimpleSetField
         * which passes through every key.
         * (e.g. for key1=value1 key2.sub2=value2 key1.sub=value3
         * it will provide key1,key2.sub2,key1.sub)
         * @param a prefix to put BEFORE every key
         * (e.g. for key1=value, if the iterator is created with prefix "aPrefix",
         * it will provide aPrefixkey1
         */
        public KeyIterator(String prefix) {
            synchronized(SimpleFieldSet.this) {
                valuesIterator = values.keySet().iterator();
                if(subsets != null)
                    subsetIterator = subsets.keySet().iterator();
                else
                    subsetIterator = null;
                while(true) {
                    if(valuesIterator != null && valuesIterator.hasNext()) break;
                    if(subsetIterator == null || !subsetIterator.hasNext()) break;
                    String name = (String) subsetIterator.next();
                    if(name == null) continue;
                    SimpleFieldSet fs = (SimpleFieldSet) subsets.get(name);
                    if(fs == null) continue;
                    String newPrefix = prefix + name + MULTI_LEVEL_CHAR;
                    subIterator = fs.keyIterator(newPrefix);
                    if(subIterator.hasNext()) break;
                    subIterator = null;
                }
                this.prefix = prefix;
            }
        }
        
        public boolean hasNext() {
            synchronized(SimpleFieldSet.this) {
                while(true) {
                    if(valuesIterator.hasNext()) return true;
                    if((subIterator != null) && subIterator.hasNext()) return true;
                    if(subIterator != null) subIterator = null;
                    if(subsetIterator != null && subsetIterator.hasNext()) {
                        String key = (String) subsetIterator.next();
                        SimpleFieldSet fs = (SimpleFieldSet) subsets.get(key);
                        String newPrefix = prefix + key + MULTI_LEVEL_CHAR;
                        subIterator = fs.keyIterator(newPrefix);
                    } else
                        return false;
                }
            }
        }
        
        public final Object next() {
            return nextKey();
        }
        
        public String nextKey() {
            synchronized(SimpleFieldSet.this) {
                String ret = null;
                if(valuesIterator != null && valuesIterator.hasNext()) {
                    return prefix + valuesIterator.next();
                }
                // Iterate subsets.
                while(true) {
                    if(subIterator != null && subIterator.hasNext()) {
                        // If we have a retval, and we have a next value, return
                        if(ret != null) return ret;
                        ret = (String) subIterator.next();
                        if(subIterator.hasNext())
                            // If we have a retval, and we have a next value, return
                            if(ret != null) return ret;
                    }
                    // Otherwise, we need to get a new subIterator (or hasNext() will return false)
                    subIterator = null;
                    if(subsetIterator != null && subsetIterator.hasNext()) {
                        String key = (String) subsetIterator.next();
                        SimpleFieldSet fs = (SimpleFieldSet) subsets.get(key);
                        String newPrefix = prefix + key + MULTI_LEVEL_CHAR;
                        subIterator = fs.keyIterator(newPrefix);
                    } else {
                        // No more subIterator's
                        if(ret == null)
                            logger.log(Level.SEVERE, "Returning null from KeyIterator.nextKey() - should never happen!");
                        return ret;
                    }
                }
            }
        }
        
        public synchronized void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    /** Tolerant put(); does nothing if fs is empty */
    public void tput(String key, SimpleFieldSet fs) {
        if(fs == null || fs.isEmpty()) return;
        put(key, fs);
    }
    
    public void put(String key, SimpleFieldSet fs) {
        if(fs == null) return; // legal no-op, because used everywhere
        if(fs.isEmpty()) // can't just no-op, because caller might add the FS then populate it...
            throw new IllegalArgumentException("Empty");
        if(subsets == null)
            subsets = new HashMap();
        if(subsets.containsKey(key))
            throw new IllegalArgumentException("Already contains "+key+" but trying to add a SimpleFieldSet!");
        if(!shortLived) key = key.intern();
        subsets.put(key, fs);
    }
    
    public synchronized void removeValue(String key) {
        int idx;
        if((idx = key.indexOf(MULTI_LEVEL_CHAR)) == -1) {
            values.remove(key);
        } else {
            if(subsets == null) return;
            String before = key.substring(0, idx);
            String after = key.substring(idx+1);
            SimpleFieldSet fs = (SimpleFieldSet) (subsets.get(before));
            if(fs == null) {
                return;
            }
            fs.removeValue(after);
            if(fs.isEmpty()) {
                subsets.remove(before);
                if(subsets.isEmpty())
                    subsets = null;
            }
        }
    }
    
    /**
     * It removes the specified subset.
     * For example, in a SimpleFieldSet like this:
     * foo=bar
     * foo.bar=foobar
     * foo.bar.boo=foobarboo
     * calling it with the parameter "foo"
     * means to drop the second and the third line.
     * @param is the subset to remove
     */
    public synchronized void removeSubset(String key) {
        if(subsets == null) return;
        int idx;
        if((idx = key.indexOf(MULTI_LEVEL_CHAR)) == -1) {
            subsets.remove(key);
        } else {
            String before = key.substring(0, idx);
            String after = key.substring(idx+1);
            SimpleFieldSet fs = (SimpleFieldSet) (subsets.get(before));
            if(fs == null) {
                return;
            }
            fs.removeSubset(after);
            if(fs.isEmpty()) {
                subsets.remove(before);
                if(subsets.isEmpty())
                    subsets = null;
            }
        }
    }
    
    /** Is this SimpleFieldSet empty? */
    public boolean isEmpty() {
        return values.isEmpty() && (subsets == null || subsets.isEmpty());
    }
    
    public Iterator directSubsetNameIterator() {
        return (subsets == null) ? null : subsets.keySet().iterator();
    }
    
    public String[] namesOfDirectSubsets() {
        return (subsets == null) ? EMPTY_STRING_ARRAY : (String[]) subsets.keySet().toArray(new String[subsets.size()]);
    }
    
    public static SimpleFieldSet readFrom(InputStream is, boolean allowMultiple, boolean shortLived) throws IOException {
        BufferedInputStream bis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        
        try {
            bis = new BufferedInputStream(is);
            try {
                isr = new InputStreamReader(bis, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                System.out.println("Impossible: "+e);
                e.getStackTrace();
                is.close();
                return null;
            }
            br = new BufferedReader(isr);
            SimpleFieldSet fs = new SimpleFieldSet(br, allowMultiple, shortLived);
            br.close();
            
            return fs;
        } finally {
            Closer.close(br);
            Closer.close(isr);
            Closer.close(bis);
        }
    }
    
    public static SimpleFieldSet readFrom(File f, boolean allowMultiple, boolean shortLived) throws IOException {
        return readFrom(new FileInputStream(f), allowMultiple, shortLived);
    }
    
    /** Write to the given OutputStream, close it and flush it. */
    public void writeTo(OutputStream os) throws IOException {
        BufferedOutputStream bos = null;
        OutputStreamWriter osw = null;
        BufferedWriter bw = null;
        
        try {
            bos = new BufferedOutputStream(os);
            try {
                osw = new OutputStreamWriter(bos, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.log(Level.SEVERE, "Impossible: " + e, e);
                os.close();
                return;
            }
            bw = new BufferedWriter(osw);
            writeTo(bw);
            // close() calls flush() but IGNORES ALL ERRORS!
            bw.flush();
            bw.close();
        }finally {
            Closer.close(bw);
            Closer.close(osw);
            Closer.close(bos);
        }
    }
    
    public int getInt(String key, int def) {
        String s = get(key);
        if(s == null) return def;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }
    
    public int getInt(String key) throws FSParseException {
        String s = get(key);
        if(s == null) throw new FSParseException("No key "+key);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new FSParseException("Cannot parse "+s+" for integer "+key);
        }
    }
    
    public double getDouble(String key, double def) {
        String s = get(key);
        if(s == null) return def;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }
    
    public double getDouble(String key) throws FSParseException {
        String s = get(key);
        if(s == null) throw new FSParseException("No key "+key);
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new FSParseException("Cannot parse "+s+" for integer "+key);
        }
    }
    
    public long getLong(String key, long def) {
        String s = get(key);
        if(s == null) return def;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }
    
    public long getLong(String key) throws FSParseException {
        String s = get(key);
        if(s == null) throw new FSParseException("No key "+key);
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new FSParseException("Cannot parse "+s+" for long "+key);
        }
    }
    
    public short getShort(String key) throws FSParseException {
        String s = get(key);
        if(s == null) throw new FSParseException("No key "+key);
        try {
            return Short.parseShort(s);
        } catch (NumberFormatException e) {
            throw new FSParseException("Cannot parse "+s+" for short "+key);
        }
    }
    
    public short getShort(String key, short def) {
        String s = get(key);
        if(s == null) return def;
        try {
            return Short.parseShort(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }
    
    public char getChar(String key) throws FSParseException {
        String s = get(key);
        if(s == null) throw new FSParseException("No key "+key);
        if (s.length() == 1)
            return s.charAt(0);
        else
            throw new FSParseException("Cannot parse "+s+" for char "+key);
    }
    
    public char getChar(String key, char def) {
        String s = get(key);
        if(s == null) return def;
        if (s.length() == 1)
            return s.charAt(0);
        else
            return def;
    }
    
    public boolean getBoolean(String key, boolean def) {
        return Fields.stringToBool(get(key), def);
    }
    
    public boolean getBoolean(String key) throws FSParseException {
        try {
            return Fields.stringToBool(get(key));
        } catch(NumberFormatException e) {
            throw new FSParseException(e);
        }
    }
    
    public void put(String key, int[] value) {
        // FIXME this could be more efficient...
        removeValue(key);
        for(int i=0;i<value.length;i++)
            putAppend(key, Integer.toString(value[i]));
    }
    
    public int[] getIntArray(String key) {
        String[] strings = getAll(key);
        if(strings == null) return null;
        int[] ret = new int[strings.length];
        for(int i=0;i<strings.length;i++) {
            try {
                ret[i] = Integer.parseInt(strings[i]);
            } catch (NumberFormatException e) {
                logger.log(Level.SEVERE, "Cannot parse "+strings[i]+" : "+e, e);
                return null;
            }
        }
        return ret;
    }
    
    public void putOverwrite(String key, String[] strings) {
        putOverwrite(key, unsplit(strings));
    }
    
    public String getString(String key) throws FSParseException {
        String s = get(key);
        if(s == null) throw new FSParseException("No such element "+key);
        return s;
    }
    
}
