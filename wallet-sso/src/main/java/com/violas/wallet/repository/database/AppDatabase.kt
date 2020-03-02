package com.violas.wallet.repository.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.violas.wallet.repository.database.converter.DateConverter
import com.violas.wallet.repository.database.dao.AccountDao
import com.violas.wallet.repository.database.dao.AddressBookDao
import com.violas.wallet.repository.database.dao.ApplySSORecordDao
import com.violas.wallet.repository.database.dao.TokenDao
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.AddressBookDo
import com.violas.wallet.repository.database.entity.ApplySSORecordDo
import com.violas.wallet.repository.database.entity.TokenDo

@Database(
    entities = [AccountDO::class, TokenDo::class, AddressBookDo::class, ApplySSORecordDo::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun tokenDao(): TokenDao
    abstract fun addressBookDao(): AddressBookDao
    abstract fun applySsoRecordDao(): ApplySSORecordDao

    companion object {
        private const val DATABASE_NAME = "timeDb"
        @Volatile
        private var sInstance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return sInstance ?: synchronized(this) {
                sInstance ?: buildDatabase(context).also { sInstance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
//                        val request = OneTimeWorkRequestBuilder<InitDatabaseWorker>().build()
//                        WorkManager.getInstance(context).enqueue(request)
                    }
                })
                .addMigrations(migration1To2())
                .build()
        }

        private fun migration1To2() = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val sql = """
                CREATE TABLE IF NOT EXISTS apply_sso_record(
	                'id' INTEGER PRIMARY KEY,
	                'account_id' INTEGER NOT NULL,
	                'child_number' INTEGER NOT NULL,
	                'wallet_address' TEXT NOT NULL,
	                'token_address' TEXT NOT NULL,
	                'status' INTEGER NOT NULL
                )
                """
                database.execSQL(sql)
                database.execSQL("CREATE UNIQUE INDEX 'index_apply_sso_record_account_id_child_number' ON apply_sso_record('account_id','child_number')")
            }
        }
    }
}