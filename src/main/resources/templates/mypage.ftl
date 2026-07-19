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
            <td>第${event.number}回</td>
            <td><a href="/g/${guildId?c}/events/${event.id?c}">${event.name}</a></td>
            <td>${event.date}</td>
            <td><#if event.isRecruiting>募集中<#else>アーカイブ済</#if></td>
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
            <td>${payment.eventName}</td>
            <td>${payment.shopName}</td>
            <td>${payment.amountText}</td>
            <td>${payment.receivedAt}</td>
          </tr>
        </#list>
      </tbody>
    </table>
    <p class="total">合計: ${totalText}</p>
  </#if>
</section>
<#include "_footer.ftl">
