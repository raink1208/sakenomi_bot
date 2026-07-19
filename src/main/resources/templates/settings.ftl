<#include "_header.ftl">
<section class="form-card">
  <h1>設定</h1>
  <#if message??>
    <p class="success">${message}</p>
  </#if>
  <form method="post" action="/g/${guildId?c}/settings">
    <label>
      イベントカテゴリ
      <select name="event-category" required>
        <option value="">選択してください</option>
        <#list categories as c>
          <option value="${c.id?c}" <#if current["event-category"]?? && current["event-category"] == c.id>selected</#if>>${c.name}</option>
        </#list>
      </select>
    </label>
    <label>
      アーカイブカテゴリ
      <select name="archive-category" required>
        <option value="">選択してください</option>
        <#list categories as c>
          <option value="${c.id?c}" <#if current["archive-category"]?? && current["archive-category"] == c.id>selected</#if>>${c.name}</option>
        </#list>
      </select>
    </label>
    <label>
      運営ロール
      <select name="admin-role" required>
        <option value="">選択してください</option>
        <#list roles as r>
          <option value="${r.id?c}" <#if current["admin-role"]?? && current["admin-role"] == r.id>selected</#if>>${r.name}</option>
        </#list>
      </select>
    </label>
    <label>
      ログチャンネル
      <select name="log-channel" required>
        <option value="">選択してください</option>
        <#list channels as ch>
          <option value="${ch.id?c}" <#if current["log-channel"]?? && current["log-channel"] == ch.id>selected</#if>>${ch.name}</option>
        </#list>
      </select>
    </label>
    <label>
      募集チャンネル
      <select name="recruit-channel" required>
        <option value="">選択してください</option>
        <#list channels as ch>
          <option value="${ch.id?c}" <#if current["recruit-channel"]?? && current["recruit-channel"] == ch.id>selected</#if>>${ch.name}</option>
        </#list>
      </select>
    </label>
    <label>
      次回イベント番号(第n回)
      <input type="number" name="next-number" min="1" value="${nextNumber?c}">
    </label>
    <button type="submit" class="button">保存</button>
  </form>
</section>
<#include "_footer.ftl">
