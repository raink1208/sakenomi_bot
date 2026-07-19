<#include "_header.ftl">
<section class="form-card">
  <h1>⚠️ イベント削除の確認</h1>
  <p>
    <strong>第${event.number}回 ${event.name}</strong> を削除します。<br>
    チャンネル・ロール・出欠・支払記録がすべて削除されます。この操作は取り消せません。
  </p>
  <div class="actions">
    <form method="post" action="/g/${guildId?c}/events/${event.id?c}/delete" class="inline-form">
      <button type="submit" class="button danger">削除する</button>
    </form>
    <a class="button secondary" href="/g/${guildId?c}/events/${event.id?c}">キャンセル</a>
  </div>
</section>
<#include "_footer.ftl">
