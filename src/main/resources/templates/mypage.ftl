<#include "_header.ftl">
<section>
  <h1>マイページ</h1>

  <h2>参加履歴</h2>
  <#if events?size == 0>
    <p class="empty">参加したイベントはありません。</p>
  <#else>
    <table class="event-table">
      <thead><tr><th>第n回</th><th>名前</th><th>開催日</th><th>状態</th></tr></thead>
      <tbody>
        <#list events as event>
          <tr>
            <td data-label="第n回">第${event.number}回</td>
            <td data-label="名前" class="primary-cell"><a href="/g/${guildId?c}/events/${event.id?c}">${event.name}</a></td>
            <td data-label="開催日">${event.date}</td>
            <td data-label="状態"><#if event.isRecruiting>募集中<#else>アーカイブ済</#if></td>
          </tr>
        </#list>
      </tbody>
    </table>
  </#if>

  <h2>支払記録</h2>
  <#if payments?size == 0>
    <p class="empty">支払記録はありません。</p>
  <#else>
    <table class="payment-table">
      <thead><tr><th>イベント</th><th>お店</th><th>金額</th><th>受領日時</th></tr></thead>
      <tbody>
        <#list payments as payment>
          <tr>
            <td data-label="イベント" class="primary-cell">${payment.eventName}</td>
            <td data-label="お店">${payment.shopName}</td>
            <td data-label="金額">${payment.amountText}</td>
            <td data-label="受領日時">${payment.receivedAt}</td>
          </tr>
        </#list>
      </tbody>
    </table>
    <p class="total">合計: ${totalText}</p>
  </#if>
</section>
<#include "_footer.ftl">
