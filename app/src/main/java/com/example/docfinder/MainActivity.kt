package com.example.docfinder

import android.app.*
import android.os.Bundle
import android.content.*
import android.net.Uri
import android.provider.OpenableColumns
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import com.google.android.material.navigation.NavigationBarView
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.*
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser

class MainActivity : AppCompatActivity() {
    private lateinit var db: DocDb
    private lateinit var container: FrameLayout
    private lateinit var nav: NavigationBarView
    private val PICK = 100

    override fun onCreate(b: Bundle?) {
        super.onCreate(b); setContentView(R.layout.activity_main)
        PDFBoxResourceLoader.init(applicationContext); db=DocDb(this)
        container=findViewById(R.id.container); nav=findViewById(R.id.navigation)
        nav.setOnItemSelectedListener { item -> if(item.itemId==R.id.nav_search) showSearch() else showDocs(); true }
        showSearch()
    }

    private fun showSearch() {
        container.removeAllViews(); val v=layoutInflater.inflate(R.layout.tab_search,container,false); container.addView(v)
        val adapter=SearchAdapter(); val list=v.findViewById<RecyclerView>(R.id.results)
        list.layoutManager=LinearLayoutManager(this); list.adapter=adapter
        val search={ 
            val q=v.findViewById<EditText>(R.id.query).text.toString().trim()
            val rows=db.searchFts(q); adapter.set(rows)
            v.findViewById<TextView>(R.id.resultCount).text=if(q.isBlank()) "Masukkan kata untuk mulai mencari" else "${rows.size} hasil ditemukan"
        }
        v.findViewById<Button>(R.id.searchBtn).setOnClickListener{search()}
        v.findViewById<EditText>(R.id.query).setOnEditorActionListener{_,_,_->search();true}
    }

    private fun showDocs() {
        container.removeAllViews(); val v=layoutInflater.inflate(R.layout.tab_upload,container,false); container.addView(v)
        val adapter=DocAdapter(); val list=v.findViewById<RecyclerView>(R.id.docs)
        list.layoutManager=LinearLayoutManager(this); list.adapter=adapter
        fun refresh(){val all=db.all(); adapter.set(all); v.findViewById<TextView>(R.id.docSummary).text="${all.size} dokumen tersimpan • indeks FTS aktif"}
        refresh()
        v.findViewById<Button>(R.id.uploadBtn).setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
                type="*/*"; putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true); addCategory(Intent.CATEGORY_OPENABLE)
            },PICK)
        }
    }

    override fun onActivityResult(r:Int,c:Int,d:Intent?){
        super.onActivityResult(r,c,d); if(r!=PICK||c!=RESULT_OK||d==null)return
        val uris=mutableListOf<Uri>(); d.data?.let{uris.add(it)}; d.clipData?.let{x->for(i in 0 until x.itemCount)uris.add(x.getItemAt(i).uri)}
        uris.distinct().forEach{importDoc(it)}; showDocs()
    }

    private fun importDoc(uri:Uri){
        try{
            val name=displayName(uri); val text=extract(uri,name)
            if(text.isBlank())throw Exception("Tidak ada teks yang dapat diekstrak")
            db.insert(name,uri.toString(),text); Toast.makeText(this,"Terindeks: $name",Toast.LENGTH_SHORT).show()
        }catch(e:Exception){Toast.makeText(this,"Gagal: ${e.message}",Toast.LENGTH_LONG).show()}
    }

    private fun displayName(uri:Uri):String{
        contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())return it.getString(0)}
        return uri.lastPathSegment?:"dokumen"
    }

    private fun extract(uri:Uri,name:String):String{
        val lower=name.lowercase()
        contentResolver.openInputStream(uri).use{input->
            if(lower.endsWith(".txt")||lower.endsWith(".csv")||lower.endsWith(".md"))return input!!.bufferedReader().readText()
            if(lower.endsWith(".pdf")){val doc=PDDocument.load(input);doc.use{return PDFTextStripper().getText(doc)}}
            if(lower.endsWith(".docx"))return extractDocx(input!!)
        }; throw Exception("Format belum didukung")
    }

    private fun extractDocx(input:InputStream):String{
        val sb=StringBuilder();val zip=ZipInputStream(BufferedInputStream(input));var e=zip.nextEntry
        while(e!=null){if(e.name=="word/document.xml"){val p=android.util.Xml.newPullParser();p.setInput(zip,"UTF-8");while(p.eventType!=XmlPullParser.END_DOCUMENT){if(p.eventType==XmlPullParser.TEXT)sb.append(p.text).append(' ');p.next()}};e=zip.nextEntry}
        return sb.toString().replace("\\s+".toRegex()," ").trim()
    }
}

class SearchAdapter:RecyclerView.Adapter<SearchVH>(){
    private var data=listOf<DocRow>()
    fun set(x:List<DocRow>){data=x;notifyDataSetChanged()}
    override fun onCreateViewHolder(p:ViewGroup,t:Int)=SearchVH(LayoutInflater.from(p.context).inflate(R.layout.item_result,p,false))
    override fun onBindViewHolder(h:SearchVH,i:Int){h.title.text=data[i].name;h.snippet.text=data[i].snippet}
    override fun getItemCount()=data.size
}
class SearchVH(v:View):RecyclerView.ViewHolder(v){val title=v.findViewById<TextView>(R.id.title);val snippet=v.findViewById<TextView>(R.id.snippet)}

class DocAdapter:RecyclerView.Adapter<DocVH>(){
    private var data=listOf<DocRow>()
    fun set(x:List<DocRow>){data=x;notifyDataSetChanged()}
    override fun onCreateViewHolder(p:ViewGroup,t:Int)=DocVH(LayoutInflater.from(p.context).inflate(R.layout.item_doc,p,false))
    override fun onBindViewHolder(h:DocVH,i:Int){val d=data[i];h.name.text=d.name;h.meta.text="${d.text.length} karakter • indeks FTS";h.del.setOnClickListener{val a=h.itemView.context as MainActivity;a.db.delete(d.id);set(a.db.all())}}
    override fun getItemCount()=data.size
}
class DocVH(v:View):RecyclerView.ViewHolder(v){val name=v.findViewById<TextView>(R.id.name);val meta=v.findViewById<TextView>(R.id.meta);val del=v.findViewById<Button>(R.id.delete)}

data class DocRow(val id:Long,val name:String,val uri:String,val text:String,val snippet:String=text.take(280))

class DocDb(c:Context):android.database.sqlite.SQLiteOpenHelper(c,"docs.db",null,2){
    override fun onCreate(d:android.database.sqlite.SQLiteDatabase){
        d.execSQL("CREATE TABLE docs(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,uri TEXT,text TEXT NOT NULL)")
        d.execSQL("CREATE VIRTUAL TABLE docs_fts USING fts5(name,text,content='docs',content_rowid='id')")
        d.execSQL("CREATE TRIGGER docs_ai AFTER INSERT ON docs BEGIN INSERT INTO docs_fts(rowid,name,text) VALUES(new.id,new.name,new.text); END")
        d.execSQL("CREATE TRIGGER docs_ad AFTER DELETE ON docs BEGIN INSERT INTO docs_fts(docs_fts,rowid,name,text) VALUES('delete',old.id,old.name,old.text); END")
        d.execSQL("CREATE TRIGGER docs_au AFTER UPDATE ON docs BEGIN INSERT INTO docs_fts(docs_fts,rowid,name,text) VALUES('delete',old.id,old.name,old.text); INSERT INTO docs_fts(rowid,name,text) VALUES(new.id,new.name,new.text); END")
    }
    override fun onUpgrade(d:android.database.sqlite.SQLiteDatabase,o:Int,n:Int){
        if(o<2){d.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS docs_fts USING fts5(name,text,content='docs',content_rowid='id')");d.execSQL("INSERT INTO docs_fts(rowid,name,text) SELECT id,name,text FROM docs")}
    }
    fun insert(n:String,u:String,t:String){writableDatabase.execSQL("INSERT INTO docs(name,uri,text) VALUES(?,?,?)",arrayOf(n,u,t))}
    fun delete(id:Long){writableDatabase.delete("docs","id=?",arrayOf(id.toString()))}
    fun all():List<DocRow>{val a=mutableListOf<DocRow>();readableDatabase.rawQuery("SELECT id,name,uri,text FROM docs ORDER BY id DESC",null).use{c->while(c.moveToNext())a.add(DocRow(c.getLong(0),c.getString(1),c.getString(2),c.getString(3)))};return a}
    fun searchFts(q:String):List<DocRow>{
        if(q.isBlank())return emptyList()
        val safe=q.trim().split(Regex("\\s+")).filter{it.isNotBlank()}.joinToString(" "){"""+it.replace(""","""")+"""}
        val a=mutableListOf<DocRow>()
        readableDatabase.rawQuery("SELECT d.id,d.name,d.uri,d.text FROM docs_fts f JOIN docs d ON d.id=f.rowid WHERE docs_fts MATCH ? ORDER BY rank LIMIT 200",arrayOf(safe)).use{c->
            while(c.moveToNext()){val t=c.getString(3);val pos=t.lowercase().indexOf(q.lowercase().split(" ").first());val s=if(pos>=0)t.substring(maxOf(0,pos-100),minOf(t.length,pos+220)) else t.take(280);a.add(DocRow(c.getLong(0),c.getString(1),c.getString(2),t,s))}
        };return a
    }
}
