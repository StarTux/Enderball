package com.cavetale.enderball;

import com.cavetale.enderball.util.Items;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

@SuppressWarnings("LineLength")
public enum Nation {
    DENMARK("Denmark", "H4sIAAAAAAAAAC3KsQ6CQBBF0QdoWNbEysqSCuMfWKJ2xlgQWzPCSjbirJkdC/7eEL3dSa4FMiwOpHR1En1gwK4NUt9h9fLsWqGH7sR1tzsxO7HIlHqLvPPxPdBYYHYK4gyABKastue62ZSwWNZDaJ9HVq9jQ30BcyFVJxzt9BrkfyONbYb5PgxB8CuZ+GFN8AUlcpDTnwAAAA==", '\uE10D'),
    UNITED_KINGDOM("UK", "H4sIAAAAAAAAAJ3OvQrCQBAE4DGHGE9IZ+OjWPrTW4itbC5nOLzswd6m8O01YGEKU2S74WOGtYDB5kRKNy85JAbsrkQRGmy7wN4JPXRfx97fa2L2YmGUWovqEJN7nlmDvq7UrlFeSNULZ4vPRonVN8M0MRssjykmwXAjkwmb1yuc/FA1ponWf+pmUXajN4DFEHvWAm+OsPJfdgEAAA==", '\uE106'),
    GERMANY("Germany", "H4sIAAAAAAAAAE3Juw7CMAxG4b8JiBAkWJh4FEYuOwNiRaY1VdTgSI4ZeHuKxNDhDEdfBDxWJzK6sdZUBIi7AJc6bF9JuFV62l65uz9IhDXCG/UR60Mu7XAWS/a5Ur9EuJAZq9QIwAUs/g+Xq8f8WHLRUTaYkk5pNtb89i3W4AtmrsszmQAAAA==", '\uE110'),
    AUSTRIA("Austria", "H4sIAAAAAAAAAFXJsQrCMBAG4N8EMUbo6OCjOKrdO4irnDXWo/EClxPx7VVw0PHji4DHYkdGh6SViwBxFeD4jOWNJfVKF1s/rmzpeCKRpBHeaIhoNrn0YyvG9tzTMEfoyCyp1AjABcy+hsvVY7otueh7GvyW/hcw+fAu5vACZm89qJsAAAA=", '\uE10B'),
    BRAZIL("Brazil", "H4sIAAAAAAAAAFXJMQvCMBCG4c9EMU3B0cGf4qh2dxBXOeMZgu0Frufgv7eCgw7v8PJEwKM9kNGZdSxVgLgJcOWG9VCEk9LdtlmZ5XIlEdYIb5QjVru+pkcnVux1otwgHMmMVcYIwAUsvw83qMdiX/uqk8zxR+mH2qnZZ59iDm8X07OImwAAAA==", (char) 0),
    BELGIUM("Belgium", "H4sIAAAAAAAAAE3JuwoCMRBG4d8EMUZYS8FHsfTSW4itzK7jEowzkIzIvr0rWGxxisMXAY/VkYyuXGpSAeI2wKU7Nq8k3BV62G7gnPVza0mES4Q36iOafdbueRJLNlyoXyKcyYyL1AjABSz+D2fVY37QrGWUNabUTqkZm/32LebwBRvSzgycAAAA", '\uE10C'),
    CANADA("Canada", "H4sIAAAAAAAAAHXNPQvCMBSF4WOCtEbo6OBPcfRjdxBXuY2xDTY3kFwV/70KDu2Q8fBweA2gsdyT0Nml7CMDZl1D+StWwbOziW6yefVe3KUlZpcMtFBn0GyHaO8HFi/vE3UL1EcScYmzAVDVqP4byiaN+S4OMX2lwZhCLpG2bRwZJrdnX6RgizEpxlQ7JWD2mw8WhQ8OR93KIwEAAA==", (char) 0),
    FRANCE("France", "H4sIAAAAAAAAAE3JMQvCMBCG4c8EMU2ho4M/xVHt7iCucq1nDdYLXE7Ef28Fhw7v8PJEwKM+kNGZtaQsQNwEuHTF+pmEe6Wbbd/3ZHzpSIQ1whsNEc1uzP2jFUv2OdFQIRzJjFVKBOACVv+Hs+Kx3Ocx6yQ15tTNqZla/PYl5vAFRMyVH5sAAAA=", '\uE10F'),
    IRELAND("Ireland", "H4sIAAAAAAAAAE3JsQrCMBAG4N+EYhrB0cFHcdS6O4irXOtZj9YLJCfi21vEoePHFwGPVUNGF85FkgJxG+Dkhs1TlLtMd9u9H2J8bUmVc4Q36iPW+zF1w1FN7HOmvkY4kRlnLRGAC1j+DdcWj+qQxpSnWWBeNq/q1xNfag5f4NQhi5sAAAA=", '\uE111'),
    MEXICO("Mexico", "H4sIAAAAAAAAAFXMuwoCMRSE4TFBjFG3tPBRLL30FmIrZ2PcDe6eQHJEfHsjWMRiip8PxgIaiwMJXXzKITJgNwYq3LAeA3uX6C7bVx/EX1ti9slCC3UWzW6I7nFkCfI+UzeHOZGIT5wtyqnB7NdQkjWm+zjEVKRBTW1Nqz8aXUXLssk3nywKH4a3gJm2AAAA", '\uE108'),
    NETHERLANDS("Netherlands", "H4sIAAAAAAAAAI3JvQ5BQRAG0M8useYmt1R4FKWfXiFaGde6NtZsMjsi3h6JQiXKk0OAR7Ni413UmooANAtw6YjpNUnslE82v5+Txf2BRaISvHFPaBe5dJe1WLLHlvsJwobNokolAMOA8cdwWj1Gy5KLvqbFn5W/q/lVwODNm5jDE+IRw2vRAAAA", (char) 0),
    POLAND("Poland", "H4sIAAAAAAAAAE3JuwrCQBBG4d9dxHWElBY+iqWX3kJsZYxrdkmchckY8e01YJHTHT4CPFYHNr5E7XMRgDYBLt+xfmaJtfLDtu+ULV5vLBKV4I0bQrXrSt0exbJ9ztwsEU5sFlV6AuACFv+HH5J6zPelK/qjChNzQ5rQ2Gzcl5jDFxCI6COcAAAA", '\uE114'),
    RUSSIA("Russia", "H4sIAAAAAAAAAKXJwQ4BMRAG4F8bVDfZo4NHcWT37iCuMqrWRE2TdkS8PRIHZ45fPg9YNB0p7WKpnAXwCwfDR8yvLDEUOunyfmaN+wOJxOJhlQaPdpVyuPSirI8tDTO4DanGItUDmDhMP4ZJ1WK8zimX17T4v8J3NT8WMHrzJmrwBIAiSiUHAQAA", (char) 0),
    SPAIN("Spain", "H4sIAAAAAAAAAHXJvQrCMBQG0M/GYo3SVfBRHP3ZHcRVbmtqo+m9kF6Rvr0KDungeDgWMFjsSensYu+FAbsukPkrVp1nV0dqdDO4EOR1qYjZRQujdLMot0Hqx4HV63Ci2xzFkVRd5N4CyAvMfobp5G6Q7yRI/NQS6TVB/l7bVslN08tCn1Q5qjguYPLlkzXDG+0i9RvwAAAA", '\uE107'),
    PORTUGAL("Portugal", "H4sIAAAAAAAAAH3MsQrCMBSF4dNGMUbUxclHcWx1dxBXubZpCcYbuLkOvr0RHDo5nOHng+MAg9WRlK5eckgMuL1FHXrsnoF9JzToQXx/uxOzFwejNDpsmpi6x4k16PtC4xL2TKpeODuUS4vFr1FrNpi3KSYpssaEzBDTxLZ/bFZWffPFWuED2z7D7bYAAAA=", (char) 0),
    SWEDEN("Sweden", "H4sIAAAAAAAAAJ3MsQrCMBSF4WNiMabgKvgojtruDuIqtzG2wXgDyRXp21vBwVEczvDzwbGARt2Q0MnnEhIDdmOgwgXre2DvMl1lO/oY0/PcEbPPFlqot1jtYnK3liXIeKR+CXMgEZ+5WACVweLTUFI0qn2KKU9S4zfSw9D9Y8p9X86nzd75YFF4AbvWklrvAAAA", '\uE115'),
    UKRAINE("Ukraine", "H4sIAAAAAAAAAH3JPQoCMRAG0M9EMUbwAHoTS396C7FdZtcxOxgnkJ0VvL0K1paPFwGP5YGMLlwHKQrEdYCTKzYPUe4q3WybJfXWtHnkpiVVrhHeKEWsdrl096Oa2OtMaYFwIjOuOkQALmD+M9yz95jtSy71M1P8KWDy5ajm8AZ1xLEMoAAAAA==", (char) 0),
    USA("USA", "H4sIAAAAAAAAAE2JvQrCQBAGP+8Qzw1YWfkolv70KcRW1uQMh5c92NsUvr0GLDIwxTAEeDQXNr5HrakIQIcAl3rsxySxU37ZUWP/eLJIVII3Hgi7Uy7d+yqW7HPjYYvQsllUqQTABWz+DVerx/pcclHMLJflxWp+ruacxBy+DPYRHpkAAAA=", '\uE109'),
    ITALY("Italy", "H4sIAAAAAAAAAI3JPQ8BQRAG4NcusUZcqfBTlD56hWhl7qy7jTObzI6If49EoRLlk4cAj+mGjQ9RS8oC0CLApRPm1ySxUT7b8t4li8eaRaISvHFLqFZ9bi5bsWSPPbcThB2bRZVCAIYB44/hrHiM1rnP+poZ/qz6u6pfBQzevIk5PAHLPNgt0QAAAA==", '\uE112'),
    SWITZERLAND("Switzerland", "H4sIAAAAAAAAAH3JsQrCMBRG4d+EYozQyclHcbS6O4ir3KaxBNsbuLkOvr0UHNpBz3b4PGCxPZHSLUpJmQG/dzCpw25MHIPQQw8Su3tLzFE8rFLvUR+HHJ5n1qTvK/UbuAupRuHiAVQO6+/DhGJRNXnIgqk5jb9J51QvqP1DeUHAatoXq8EH9v2cIuoAAAA=", '\uE116'),
    SOUTH_KOREA("South Korea", "H4sIAAAAAAAAAHXMvQrCMBSG4c8EaU1BFycvxdGf3UFc5aSNJbSeQHI6ePcqODRCxpcHXgNoNCcSurmYfGDA7Goo32H79OzaSA/ZR9fdLTG7aKCFeoP1YQztcGbx8rpSv0J9IREXORkAVY3q19Dj1Gksj2EM8UMNZqYkzQgZ2SLp1oai2Wy5yUzKpv6WwOKbE4vCG88mtgYkAQAA", (char) 0),
    NORWAY("Norway", "H4sIAAAAAAAAAHXJuwoCMRBG4d9EMWZhSwsfxdJLbyG2Msa4hs1OIBkR394VLGJhcYrDZwGNZkdCJ59LSAzYlYEKVyyHwN5lusn6eQ/izxdi9tlCC3UW7SYm1+9ZgryO1C1gDiTiMxcLYGow/z5ULhqzbYopj9KipvifhprwQ8VV1IxNPvtgUXgD30GWRdEAAAA=", '\uE113'),
    SCOTLAND("Scotland", "H4sIAAAAAAAAAK3JOw7CMBAFwJdYgHEkDgA3oeTTUyDaaBOMsWLW0mZdcHtAooCCjnI0DjBodqR08jLGzIBbWtTxjNUtsu+FLrpOMVy17VLxbUfMXhyMUnBYbFLuhz1r1PuRwhz2QKpeeHQAphazt2GCkMFkm1OWZzX4vvLr6l4+Cv8voHqxsFZ4AKDDdgIOAQAA", (char) 0);

    public final String name;
    public final String base64;
    public final ItemStack bannerItem;
    public final DyeColor bannerDyeColor;
    public final List<Pattern> bannerPatterns;
    public final char chatIcon;
    public final Component component;

    Nation(final String name, final String base64, final char chatIcon) {
        this.name = name;
        this.base64 = base64;
        // Icon
        this.chatIcon = chatIcon;
        this.component = (int) chatIcon > 0
            ? Component.text(chatIcon).style(Style.style().font(Key.key("cavetale:default")).color(TextColor.color(0xFFFFFF)))
            : Component.empty();
        // We copy only the material and patterns from the existing item.
        ItemStack oldItem = Items.deserialize(base64);
        BannerMeta oldMeta = (BannerMeta) oldItem.getItemMeta();
        BannerMeta meta = (BannerMeta) Bukkit.getItemFactory().getItemMeta(oldItem.getType());
        this.bannerDyeColor = Items.getDyeColor(oldItem.getType());
        this.bannerPatterns = oldMeta.getPatterns();
        meta.setPatterns(bannerPatterns);
        meta.addItemFlags(ItemFlag.values());
        meta.displayName(Component.text().append(component)
                         .append(Component.text(name, TextColor.color(0xFFFFFF)))
                         .decoration(TextDecoration.ITALIC, false).build());
        this.bannerItem = new ItemStack(oldItem.getType());
        this.bannerItem.setItemMeta(meta);
    }

    public boolean hasChatIcon() {
        return chatIcon !=  0;
    }
}
