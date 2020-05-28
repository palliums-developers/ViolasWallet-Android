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
import com.violas.wallet.repository.database.dao.TokenDao
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.AddressBookDo
import com.violas.wallet.repository.database.entity.TokenDo

@Database(
    entities = [AccountDO::class, TokenDo::class, AddressBookDo::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun tokenDao(): TokenDao
    abstract fun addressBookDao(): AddressBookDao

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
                database.execSQL("ALTER TABLE account ADD COLUMN encrypted_password BLOB")
            }
        }
    }
}